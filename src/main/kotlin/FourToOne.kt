package solve

class FourToOne : Game<Int, Int> {
    override val initialPos = 200

    override fun doMove(p: Int, m: Int): Int {
        return p - m

    }
    
    override fun genMoves(p: Int): Array<Int> {
        when {
            p == 0 -> return arrayOf()
            p == 1 -> return arrayOf(1)
            else   -> return arrayOf(1, 2)
        }
    }

    override fun primitive(p: Int): Primitive {
        if (p == 0) {
            return Primitive.LOSS
        } else {
            return Primitive.UNDECIDED
        }
    }
    
    override fun hashPosition(p: Int): Int {
        // Pretty stupid hash, but it works for this small example.
        return p
    }
}

fun main(args: Array<String>) {
    Solver(FourToOne()).run()
}
