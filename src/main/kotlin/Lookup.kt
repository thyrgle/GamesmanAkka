package solve

/**
 * Lookup object.
 *
 * No logic in this class. Lookup is meant to be interpreted by onReceive.
 */
data class Lookup<Pos>(val position: Pos,
                       val parent: Pos)
