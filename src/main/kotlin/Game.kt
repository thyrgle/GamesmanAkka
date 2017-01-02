package solve

interface Game<Pos, Move> {

    val initialPos: Pos?

    fun doMove(p: Pos, m: Move): Move

    fun genMoves(p: Pos): Array<Move>

    fun primitive(p: Pos): Primitive

    fun hashPosition(p: Pos): Int
}
