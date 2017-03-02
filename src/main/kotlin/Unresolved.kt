/**
 * Created by Henry on 2/10/2017.
 */

package solve

import akka.event.LoggingAdapter

class Unresolved<Pos>(log: LoggingAdapter) {
    private data class Data<Pos>(val childCount: Int,
                         val currentIndex: Int,
                         val state: State,
                         val parents: MutableList<Pos>)

    private val log = log

    private val map: MutableMap<Pos, Data<Pos>> = mutableMapOf()

    /**
     * Check if already unresolved
     *
     * @param position to be checked
     */
    fun isUnresolved(position: Pos) : Boolean {
        return map[position] != null
    }

    /**
     * Adds the position to this data structure, along with adding its parent ref
     *
     * @param position being added
     * @param parent of position to be kept
     */
    fun add(position: Pos, parent: Pos) {
        map[position] = Data(0, 0, State(Primitive.LOSS), mutableListOf<Pos>())
        addParent(position, parent)
    }

    /**
     * Removes the position (presumably after we finish resolving it)
     *
     * @param position to be removed
     */
    fun remove(position: Pos) {
        map.remove(position)
    }

    /**
     * Adds another parent to the position
     *
     * @param position's parent we're adding
     * @param parent to be added
     */
    fun addParent(position: Pos, parent: Pos) {
        map[position]!!.parents!!.add(parent)
    }

    /**
     * Gets the index of genMoves that we need to distribute next
     *
     * @param position being looked up
     * @return the next index
     */
    fun getCurrentIndex(position: Pos) : Int {
        return map[position]!!.currentIndex
    }

    /**
     * Gets the current state (outcome / remoteness) of a position
     *
     * @param position being looked up
     * @return the state of position
     */
    fun getState(position: Pos) : State {
        return map[position]!!.state
    }

    /**
     * Gets the current parents of this position
     *
     * @param position being looked up
     * @return the parents of this position
     */
    fun getParents(position: Pos) : MutableList<Pos> {
        return map[position]!!.parents
    }

    /**
     * Sets a new value for child count (the number of children we are waiting to be resolved)
     *
     * @param position being updated
     * @param childCount the new child count value
     */
    fun updateChildCount(position: Pos, childCount: Int) {
        map[position] = map[position]!!.copy(childCount = childCount)
    }

    /**
     * Sets a new value for current index (the index of genMoves to be distributed next)
     * @param position being updated
     * @param index the new current index value
     */
    fun updateCurrentIndex(position: Pos, index: Int) {
        map[position] = map[position]!!.copy(currentIndex = index)
    }

    /**
     * Updates a position by resolving a child
     *
     * @param position being updated
     * @param state of the child that we are resolving
     * @return whether we are waiting on more children or not
     */
    fun updateState(position: Pos, state: State) : Boolean {
        log.debug("Updating position " + position + " with " + state)
        val (outcome, remoteness) = state
        val data = map[position]

        log.debug("Before update on position " + position.toString() + ": " + data)

        if (outcome.equals(Primitive.LOSS)) {

            val currentRemoteness = data!!.state.remoteness

            val newRemoteness =
                    if(remoteness < currentRemoteness || !data!!.state.outcome.equals(Primitive.WIN))
                        //update remoteness if better found or first time we set this state to win
                        remoteness + 1
                    else
                        currentRemoteness

            map[position] = data!!.copy(state = State(Primitive.WIN, newRemoteness))

        } else if (outcome.equals(Primitive.WIN)) {

            if (data!!.state.outcome.equals(Primitive.LOSS) && remoteness + 1 > data!!.state.remoteness) {
                //update remoteness if better found and this state is still a loss
                map[position] = data!!.copy(state = State(Primitive.LOSS, remoteness + 1))
            }

        } else if (outcome.equals(Primitive.TIE)) {
            if (!data!!.state.outcome.equals(Primitive.WIN)) {
                val currentRemoteness = data!!.state.remoteness

                val newRemoteness =
                        if (remoteness + 1 > currentRemoteness || data!!.state.outcome.equals(Primitive.LOSS))
                            //update remoteness is this is first time seeing tie, or better found
                            remoteness + 1
                        else
                            currentRemoteness

                map[position] = data!!.copy(state = State(Primitive.TIE, newRemoteness))
            }
        }

        updateChildCount(position, data!!.childCount - 1)

        log.debug("After update on position " + position.toString() + " : " + map[position])

        return map[position]!!.childCount == 0
    }
}