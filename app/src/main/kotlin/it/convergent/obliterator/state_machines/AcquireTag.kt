package it.convergent.obliterator.state_machines

import android.content.Context
import android.util.Log

/**
 * Created by altamic on 02/10/16.
 */
class AcquireTagFlow(val actions: Actions, val callbacks: Callbacks) {

    val TAG: String = this.javaClass.simpleName

    // internal actions
    interface Actions {
    }

    // can be triggered externally (or trigger external behaviour)
    interface Callbacks {
        fun startAcquireCarnet()
        fun activateTagPollingCallback()
        fun deactivateTagPollingCallback()
        fun tagInRangeCallback()
        fun tagReadCallback()
        fun predecessorCarnetNotFoundCallback()
        fun predecessorCarnetFoundCallback()
        fun acquireTagCompleted()
        fun acquireTagError()
    }

    enum class State {
        START,
        POLLING_ACTIVATED,
        TAG_IN_RANGE,
        TAG_READ,
        POLL_DEACTIVATED,
        END,
        ERROR
    }

    private val transitions = mapOf(
            State.START             to arrayOf(State.POLLING_ACTIVATED),
            State.POLLING_ACTIVATED to arrayOf(State.TAG_IN_RANGE),
            State.TAG_IN_RANGE      to arrayOf(State.TAG_READ),
            State.TAG_READ          to arrayOf(State.POLL_DEACTIVATED),
            State.POLL_DEACTIVATED  to arrayOf(State.END)
    )

    private val behaviour = mapOf(
            Pair(State.START,               State.POLLING_ACTIVATED) to { callbacks.activateTagPollingCallback() },
            Pair(State.POLLING_ACTIVATED,   State.TAG_IN_RANGE)    to { callbacks.tagInRangeCallback() },
            Pair(State.TAG_IN_RANGE,        State.TAG_READ)        to { callbacks.tagReadCallback() },
            Pair(State.TAG_READ,            State.POLL_DEACTIVATED) to { callbacks.deactivateTagPollingCallback() },
            Pair(State.POLL_DEACTIVATED,    State.END)             to { callbacks.acquireTagCompleted() }
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
        next(State.POLLING_ACTIVATED)
    }

    fun error() {
        currentState = State.ERROR
        callbacks.acquireTagError()
    }
}

class AcquireTagHandler(val context: Context, val callbacks: AcquireTagFlow.Callbacks): AcquireTagFlow.Actions {
    val flow = AcquireTagFlow(this, this.callbacks)
}