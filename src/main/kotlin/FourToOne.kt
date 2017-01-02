package solve

class FourToOne : Game<Int, Int> {
    override val initialPos = 4

    override fun doMove(p: Int, m: Int): Int {
        return 0

    }
    
    override fun genMoves(p: Int): Array<Int> {
        return emptyArray<Int>()
    }

    override fun primitive(p: Int): Primitive {
        return Primitive.WIN
    }
}

class Main {
    


    fun main() {

    }
}
