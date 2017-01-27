package solve

/**
 * Interface describing what a game must conform to for the solver to use.
 * @param Pos the datatype representing a position.
 * @param Move the datatype representing a move.
 */
interface Game<Pos, Move> {
    val initialPos: Pos
    fun doMove(p: Pos, m: Move): Move
    fun genMoves(p: Pos): Array<Move>
    fun primitive(p: Pos): Primitive
    fun hashPosition(p: Pos): Int
}
