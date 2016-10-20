package solve;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.ActorRef;

public class World extends UntypedActor {
    
    @Override
    public void preStart() {
    }

    @Override
    public void onReceive(Object msg) {
        unhandled(msg);
    }
}
