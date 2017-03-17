package solve

import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedActor
import akka.event.Logging

import org.ehcache.Cache
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder

/**
 * A group of "solvers" that solve games!
 *
 * @param Pos the data type that represents a game position (or state).
 * @param Move the data type that represents a game move.
 */
class Solver<Pos, Move> (game: Game<Pos, Move>, posClass: Class<Pos>) {

    val actors: MutableList<ActorRef> = arrayListOf()
    val game: Game<Pos, Move> = game
    val posClass: Class<Pos> = posClass

    /**
     * Given a hashed number, get the appropriate actor.
     * @param hash the hashed integer.
     * @return the actor associated with the particular hash.
     */
    fun actorForHash(hash: Int): ActorRef {
        return actors.get(Math.abs(hash % Config.ACTOR_COUNT))
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
        val cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build()
        var solvedPositions: Cache<Pos, State>? = null
        val unresolved: Unresolved<Pos> = Unresolved(log)
        var counter = 0

        override fun preStart() {
            cacheManager.init()

            solvedPositions = cacheManager.createCache("solvedPositions",
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(posClass, State::class.javaObjectType, ResourcePoolsBuilder.heap(10)).build())
        }

        /**
         * Handle the lookup message
         *
         * @param position to be looked up.
         */
        fun lookup(position: Pos, parent: Pos) {
            //check if position is already waiting to be resolved, no need to distribute again
            if (unresolved.isUnresolved(position)) {
                log.debug(position.toString() + " is already being resolved")
                unresolved.addParent(position, parent)
                return
            }

            //check if position is already solved
            val solved = solvedPositions!!.get(position)
            solved?.let {
                log.debug(position.toString() + " is already solved with value " + solved)
                sendMessage(sender, Resolve(parent, solved))
                return
            }

            //check if position is a primitive
            val primitive = game.primitive(position)
            if (!primitive.equals(Primitive.UNDECIDED)) {
                log.info(position.toString() + " is a primitive with value " + primitive)
                solvedPositions!!.put(position, State(primitive, 0))
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
            val maxToSend = if (counter >= Config.MAX_DISTR_COUNT) {
                1
            } else {
                Config.MAX_DISTR_COUNT - counter
            }

            //TODO: optimize game API so we don't have to generate all the children each time
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
                //this means there are no more children, so we have solved this position
                val solvedState = unresolved.getState(position)
                solvedPositions!!.put(position, solvedState)

                log.info(position.toString() + " is a " + solvedPositions!!.get(position).toString())

                if (position!!.equals(game.initialPos)) {
                    println("We're done, result: " + solvedState)

                    //TODO: replace with post-completion procedure
                    for (actor in actors) {
                        context.stop(actor)
                    }
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
            if (unresolved.updateState(position, state)) {
                //finished resolving all children that were sent out, continue distributing next ones
                distribute(position)
            }
        }

        /**
         * Handles anything that needs to be done when sending a message out. Currently it increments counter
         *
         * @param targetActor to send message to
         * @param msg being sent
         */
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
