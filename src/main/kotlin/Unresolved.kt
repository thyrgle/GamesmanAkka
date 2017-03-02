/**
 * Created by Henry on 2/10/2017.
 */

package solve

import akka.event.LoggingAdapter
import akka.event.Logging
import java.util.*

class Unresolved<Pos>(log: LoggingAdapter) {
    data class Data<Pos>(val childCount: Int,
                         val currentIndex: Int,
                         val state: State,
                         val parents: MutableList<Pos>)

    val log = log

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

    fun getCurrentIndex(position: Pos) : Int {
        return map[position]!!.currentIndex
    }

    fun getState(position: Pos) : State {
        return map[position]!!.state
    }

    fun getParents(position: Pos) : MutableList<Pos> {
        return map[position]!!.parents
    }

    fun updateChildCount(position: Pos, childCount: Int) {
        map[position] = map[position]!!.copy(childCount = childCount)
    }

    fun updateCurrentIndex(position: Pos, index: Int) {
        map[position] = map[position]!!.copy(currentIndex = index)
    }

    fun updateState(position: Pos, state: State) : Boolean {
        log.debug("Updating position " + position + " with " + state)
        val (outcome, remoteness) = state
        val data = map[position]

        log.debug("Before update on position " + position.toString() + ": " + data)

        if (outcome.equals(Primitive.LOSS)) {

            val currentRemoteness = data!!.state.remoteness
            val newRemoteness =
                    if(remoteness < currentRemoteness || !data!!.state.outcome.equals(Primitive.WIN))
                        remoteness + 1
                    else
                        currentRemoteness

            map[position] = data!!.copy(state = State(Primitive.WIN, newRemoteness))

        } else if (outcome.equals(Primitive.WIN)) {

            if (data!!.state.outcome.equals(Primitive.LOSS) && remoteness + 1 > data!!.state.remoteness) {
                map[position] = data!!.copy(state = State(Primitive.LOSS, remoteness + 1))
            }

        } else if (outcome.equals(Primitive.TIE) && !data!!.state.outcome.equals(Primitive.WIN)) {
            map[position] = data!!.copy(state = State(Primitive.TIE))
        }

        updateChildCount(position, data!!.childCount - 1)

        log.debug("After update on position " + position.toString() + " : " + map[position])

        return map[position]!!.childCount == 0
    }
}