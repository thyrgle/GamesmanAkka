package solve;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.ActorRef;

public class World extends UntypedActor {

	private class GameTree {
		public GameState data;
		//not sure whether or not to use an array or hashmap
		public GameTree[] parents;
		public GameTree[] children;
		public GameTree(GameState data) {
			this.data = data;
        	parent = new GameTree[];
        }
        	
        //use when this gamestate's gametree was already created but another parent
        //has this gamestate as a child
        public void addParent(GameTree parent) {
        	parents.add(parent);
        }

        //prob doesnt work, just a placeholder
        public void addChildren(GameTree[] children) {
        	this.children.add(children)
        }

	}
    
    //list of all the solvers (actors)
    //its public because we need to be able to access it from other actors
    public static ActorRef[] solvers = new ActorRef[Config.ACTOR_COUNT];

    //this assumes that a hash function exists for gamestate that returns an int
    //basically gets an actor that is assigned to particular gamestates
    public ActorRef getSolver(GameState state) {
    	int hash = state.hash();
    	return solvers[hash % Config.ACTOR_COUNT];
    }

    @Override
    public void preStart() {
    	for (int i = 0; i < Config.ACTOR_COUNT; i++) {
    		final ActorRef solver = getContext()
                .actorOf(
                    Props.create(Solver.class), 
                    "solver" + Integer.toString(i));
            //havent worked out what to tell the first solvers yet
            solver.tell(0, getSelf());
            solvers[i] = solver;
    	}
    }

    @Override
    public void onReceive(Object msg) {
        unhandled(msg);
    }
}
