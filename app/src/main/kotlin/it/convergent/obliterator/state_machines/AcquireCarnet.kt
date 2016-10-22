package it.convergent.obliterator.state_machines

import android.content.Context
import android.util.Log

/**
 * Created by altamic on 02/10/16.
 */
class AcquireCarnetFlow(val actions: Actions, val callbacks: Callbacks) {

    val TAG: String = this.javaClass.simpleName

    // internal actions
    interface Actions {
    }

    // can be triggered externally (or trigger external behaviour)
    interface Callbacks {
        fun startAcquireCarnet()
        fun activatePcdModeCallback()
        fun deactivatePcdModeCallback()
        fun carnetInRangeCallback()
        fun carnetReadCallback()
        fun predecessorCarnetNotFoundCallback()
        fun predecessorCarnetFoundCallback()
        fun acquireCarnetCompleted()
        fun acquireCarnetError()
    }

    enum class State {
        START,
        WAIT_FOR_CARNET,
        CARNET_IN_RANGE,
        CARNET_READ,
        PREDECESSOR_FOUND,
        PREDECESSOR_NOT_FOUND,
        PCD_DEACTIVATED,
        END,
        ERROR
    }

    private val transitions = mapOf(
            State.START             to arrayOf(State.WAIT_FOR_CARNET),
            State.WAIT_FOR_CARNET   to arrayOf(State.CARNET_IN_RANGE),
            State.CARNET_IN_RANGE   to arrayOf(State.CARNET_READ),
            State.CARNET_READ       to arrayOf(State.PREDECESSOR_FOUND, State.WAIT_FOR_CARNET),
            State.PREDECESSOR_FOUND to arrayOf(State.END)
    )

    private val behaviour = mapOf(
            Pair(State.START,             State.WAIT_FOR_CARNET)    to { callbacks.activatePcdModeCallback() },
            Pair(State.WAIT_FOR_CARNET,   State.CARNET_IN_RANGE)    to { callbacks.carnetInRangeCallback() },
            Pair(State.CARNET_IN_RANGE,   State.CARNET_READ)        to { callbacks.carnetReadCallback() },
            Pair(State.CARNET_READ,       State.PREDECESSOR_FOUND)  to { callbacks.predecessorCarnetFoundCallback() },
            Pair(State.PREDECESSOR_NOT_FOUND, State.WAIT_FOR_CARNET)    to { callbacks.predecessorCarnetNotFoundCallback() },
            Pair(State.PREDECESSOR_FOUND, State.PCD_DEACTIVATED)    to { callbacks.deactivatePcdModeCallback() },
            Pair(State.PCD_DEACTIVATED,   State.END)                to { callbacks.acquireCarnetCompleted() }
    )

    var currentState = State.START

    private fun go(current:State, next:State) {
        Log.d(TAG, "Trying to go from $currentState to $next")
        if (transitions[current]!!.contains(next)) {
            transaction(next)
        } else {
            Log.d(TAG, "ERROR: $next state not found!")
            error()
        }
    }

    private fun transaction(next:State) {
        val oldState = currentState
        currentState = next
        val transition = behaviour[Pair(oldState, currentState)]!!
        transition.invoke()
        Log.d(TAG, "current state now is: $currentState")
    }

    fun next(state:State) {
        go(currentState, state)
    }

    fun start() {
        currentState = State.START
        next(State.WAIT_FOR_CARNET)
    }

    fun error() {
        currentState = State.ERROR
        callbacks.acquireCarnetError()
    }
}

class AcquireCarnetHandler(val context: Context, val callbacks: AcquireCarnetFlow.Callbacks): AcquireCarnetFlow.Actions {
    val flow = AcquireCarnetFlow(this, this.callbacks)
}