package solve;

import akka.actor.UntypedActor;

public class Solver extends UntypedActor {

	private String LOSS, WIN, TIE, DRAW, UNDECIDED = "LOSS", "WIN", "TIE", "DRAW", "UNDECIDED";
	private boolean solved = False;
    private HashMap known_states = new HashMap();

    @Override
    public void onReceive(Object msg) {
    	//Should recieve a gamestate
    	GameState gamestate = (GameState) msg;
    	//this part creates a new GameTree to be stored in known_states when it doesnt
    	//already exist
    	//assumes gamestate has a hash() function
    	if(!known_states.containsKey(gamestate.hash())) {
    		GameTree gametree = new GameTree(gamestate);
    		known_states.add(gamestate.hash(), gametree);
    		//still need to add the part where possible moves are generated and distributed to 
    		//other actors
    	}

    }

 }
