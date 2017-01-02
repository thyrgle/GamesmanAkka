package solve

interface Game<Move> {

    val initialPos: Move?

    fun doMove(m: Move): Move

    fun genMoves(m: Move): MutableList<Move>

    fun primitive(m: Move): Primitive
}
