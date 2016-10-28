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
        fun acquireTagCompleted()
        fun acquireTagError()
    }

    enum class State {
        START,
        ACTIVATE_POLLING,
        TAG_IN_RANGE,
        TAG_READ,
        DEACTIVATE_POLLING,
        END,
        ERROR
    }

    private val transitions = mapOf(
            State.START             to arrayOf(State.ACTIVATE_POLLING),
            State.ACTIVATE_POLLING to arrayOf(State.TAG_IN_RANGE),
            State.TAG_IN_RANGE      to arrayOf(State.TAG_READ),
            State.TAG_READ          to arrayOf(State.DEACTIVATE_POLLING),
            State.DEACTIVATE_POLLING to arrayOf(State.END)
    )

    private val behaviour = mapOf(
            Pair(State.START,              State.ACTIVATE_POLLING) to { callbacks.activateTagPollingCallback() },
            Pair(State.ACTIVATE_POLLING,   State.TAG_IN_RANGE)    to { callbacks.tagInRangeCallback() },
            Pair(State.TAG_IN_RANGE,       State.TAG_READ)        to { callbacks.tagReadCallback() },
            Pair(State.TAG_READ,           State.DEACTIVATE_POLLING) to { callbacks.deactivateTagPollingCallback() },
            Pair(State.DEACTIVATE_POLLING, State.END)             to { callbacks.acquireTagCompleted() }
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
        Log.d(TAG, "current state now is: $currentState")
        val transition = behaviour[Pair(oldState, currentState)]!!
        transition.invoke()
    }

    fun next(state:State) {
        go(currentState, state)
    }

    fun start() {
        currentState = State.START
        next(State.ACTIVATE_POLLING)
    }

    fun error() {
        currentState = State.ERROR
        callbacks.acquireTagError()
    }
}

class AcquireTagHandler(val context: Context, val callbacks: AcquireTagFlow.Callbacks): AcquireTagFlow.Actions {
    val TAG by lazy { flow.TAG }
    val flow = AcquireTagFlow(this, this.callbacks)

    fun start() { flow.start() }
    fun next(state: AcquireTagFlow.State) { flow.next(state) }
}