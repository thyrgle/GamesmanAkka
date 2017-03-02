package solve

enum class Primitive {
    WIN, LOSS, TIE, DRAW, UNDECIDED
}

data class State(val outcome: Primitive, val remoteness: Int = 0)
