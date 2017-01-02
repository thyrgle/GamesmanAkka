package solve;

class GameState<P>(pos: P) {

    var remoteness = -1;
    var state = -1;
    var pos: P = pos;

    constructor(pos: P, remoteness: Int, state: Int) : this(pos) {
        this.remoteness = remoteness;
        this.state = state;
    }

}
