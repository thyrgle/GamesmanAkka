package solve

import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedActor

class Solver<Pos, Move> : UntypedActor() {

    var actors: MutableList<ActorRef> = arrayListOf()

    fun actorForHash(hash: Int): ActorRef {
        return actors.get(hash % Config.ACTOR_COUNT)
    }

    fun run(game: Game<Pos, Move>) {
        val system = ActorSystem.create("SolverSystem")
        for (i in 1..Config.ACTOR_COUNT) {
            actors.add(
                system.actorOf(Props.create(Solver::class.java), "Solver" + i.toString())
            )
        }
        val master = actorForHash(game.hashPosition(game.initialPos))
     }

    override fun onReceive(msg: Any?) {
        when (msg) {
            is Lookup<*>     -> println("lookup")
            is Resolve<*>    -> println("resolve")
            is SendBack<*>   -> println("send back")
            is Distribute<*> -> println("distribute")
        }
    }

 }
