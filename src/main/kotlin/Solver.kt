package solve

import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedActor

import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory

/**
 * A group of "solvers" that solve games!
 *
 * @param Pos the data type that represents a game position (or state).
 * @param Move the data type that represents a game move.
 */
class Solver<Pos, Move> (game: Game<Pos, Move>) {

    val actors: MutableList<ActorRef> = arrayListOf()
    val game: Game<Pos, Move> = game;

    /**
     * Given a hashed number, get the appropriate actor.
     * @param hash the hashed integer.
     * @return the actor associated with the particular hash.
     */
    fun actorForHash(hash: Int): ActorRef {
        return actors.get(hash % Config.ACTOR_COUNT)
    }
    
    /**
     * Run the solver as a whole!
     * 
     * Responsible for initializing the actor system and then starting it off!
     *
     * @param game the game to be solved.
     */
    fun run() {
        val system = ActorSystem.create("SolverSystem")
        for (i in 0..(Config.ACTOR_COUNT - 1)) {
            actors.add(
                    system.actorOf(Props.create(SolverActor::class.java, this), "Solver" + i.toString())
            )
        }
        val master = actorForHash(game.hashPosition(game.initialPos))
        master.tell(Lookup<Pos>(game.initialPos, game.initialPos), null)
    }


    /**
     * An actor responsible for maintaining and "solving" a subset of game states.
     */
    inner class SolverActor : UntypedActor() {

        val log = Logging.getLogger(context.system(), this)

        val solvedPositions: MutableMap<Pos, State> =  mutableMapOf()
        val unresolved: Unresolved<Pos> = Unresolved(log)

        var counter = 0

        /**
         * Handle the lookup message
         *
         * @param position to be looked up.
         */
        fun lookup(position: Pos, parent: Pos) {
            //check if position is already waiting to be resolved, no need to distribute again
            if (unresolved.isUnresolved(position)) {
                log.debug("Already being resolved")
                unresolved.addParent(position, parent)
                return
            }

            //check if position is already solved
            val solved = solvedPositions.get(position)
            if (solved != null) {
                log.debug("Already solved")
                sendMessage(sender, Resolve(parent, solved))
                return
            }

            //check if position is a primitive
            val primitive = game.primitive(position)
            if (!primitive.equals(Primitive.UNDECIDED)) {
                log.debug(position.toString() + " is a primitive with value " + primitive)
                solvedPositions.put(position, State(primitive, 0))
                sendMessage(sender, Resolve(parent, State(primitive, 0)))
                return
            }

            unresolved.add(position, parent)

            distribute(position)
        }

        /**
         * Generate a positions children and distribute them to everyone.
         *
         * @param Pos he position to be distributed.
         */
        fun distribute(position: Pos) {
            val currentIndex = unresolved.getCurrentIndex(position)
            val maxToSend = Config.MAX_DISTR_COUNT - counter

            //TODO: optimize game API to use current index and # of children we're sending
            val moves = game.genMoves(position)

            var i = 0
            while (i < maxToSend && currentIndex + i < moves.size) {

                val move = moves[currentIndex + i]
                val newPosition = game.doMove(position, move)
                val hash = game.hashPosition(newPosition)

                val targetActor = actorForHash(hash)
                val msg = Lookup<Pos>(newPosition, position)

                sendMessage(targetActor, msg)

                i++
            }

            if (i == 0) {
                val solvedState = unresolved.getState(position)
                solvedPositions.put(position, solvedState)

                log.info(position.toString() + " is a " + solvedPositions.get(position).toString())

                if (position!!.equals(game.initialPos)) {
                    log.debug("We're done")
                    return
                }

                for (parent in unresolved.getParents(position)) {
                    val hash = game.hashPosition(parent)
                    val targetActor = actorForHash(hash)

                    sendMessage(targetActor, Resolve<Pos>(parent, solvedState))
                }

                unresolved.remove(position)
            }
            else {
                unresolved.updateChildCount(position, i)
                unresolved.updateCurrentIndex(position, i + currentIndex)
            }
        }

        /**
         * Handle the resolve message
         *
         * @param position to be resolved.
         */ 
        fun resolve(position: Pos, state: State) {
            // TODO.
            if (unresolved.updateState(position, state)) {
                distribute(position)
            }
        }

        fun sendMessage(targetActor: ActorRef, msg: Any) {

            log.debug("Send " + msg.toString() + " to " + targetActor.toString())
            targetActor.tell(msg, self)
            counter++
        }

        override fun onReceive(msg: Any?) {
            log.debug(msg.toString() + " received ")
            counter--

            when (msg) {
                is Lookup<*> -> lookup(msg.position as Pos, msg.parent as Pos)
                is Resolve<*> ->  resolve(msg.position as Pos, msg.state)
            }
        }
    }

 }
