package solve;

public class GameState<P> {

    private int remoteness = -1;
    private int state = -1;
    private final P pos;

    public GameState(P pos) {
        this.pos = pos;
    }

    public GameState(P pos, int remoteness, int state) {
        this.pos = pos;
        this.remoteness = remoteness;
        this.state = state;
    }

}
