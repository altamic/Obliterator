package it.convergent.obliterator.state_machines

/**
 * Created by altamic on 02/10/16.
 */
class AcquireCarnetFlow(val actions: Actions, val callbacks: Callbacks) {

    // internal actions
    interface Actions {
        fun activatePcdModeAction()
        fun carnetInRangeAction()
        fun readCarnetAction()
        fun deactivatePcdModeAction()
    }

    // can be triggered externally (or trigger external behaviour)
    interface Callbacks {
        fun start()
        fun predecessorNotFoundCallback()
        fun predecessorFoundCallback()
        fun completed()
        fun error()
    }

    enum class State {
        START,
        WAIT_FOR_CARNET,
        CARNET_IN_RANGE,
        CARNET_READ,
        PREDECESSOR_FOUND,
        PCD_DEACTIVATED,
        END,
        ERROR
    }

    val transitions = mapOf(
            State.START             to arrayOf(State.WAIT_FOR_CARNET),
            State.WAIT_FOR_CARNET   to arrayOf(State.CARNET_IN_RANGE),
            State.CARNET_IN_RANGE   to arrayOf(State.CARNET_READ),
            State.CARNET_READ       to arrayOf(State.PREDECESSOR_FOUND, State.WAIT_FOR_CARNET),
            State.PREDECESSOR_FOUND to arrayOf(State.END)
    )

    private val behaviour = mapOf(
            Pair(State.START,             State.WAIT_FOR_CARNET)    to { actions.activatePcdModeAction() },
            Pair(State.WAIT_FOR_CARNET,   State.CARNET_IN_RANGE)    to { actions.carnetInRangeAction() },
            Pair(State.CARNET_IN_RANGE,   State.CARNET_READ)        to { actions.readCarnetAction() },
            Pair(State.CARNET_READ,       State.PREDECESSOR_FOUND)  to { callbacks.predecessorFoundCallback() },
            Pair(State.CARNET_READ,       State.WAIT_FOR_CARNET)    to { callbacks.predecessorNotFoundCallback() },
            Pair(State.PREDECESSOR_FOUND, State.PCD_DEACTIVATED)    to { actions.deactivatePcdModeAction() },
            Pair(State.PCD_DEACTIVATED,   State.END)                to { callbacks.completed() }
    )

    var currentState = State.START

    private fun go(current:State, next:State) {
        if (transitions[current]!!.contains(next))
            transaction(next)
        else
            error()
    }

    private fun transaction(next:State) {
        println("Trying to go from $currentState to $next")
        val oldState = currentState
        currentState = next
        println("current state: $currentState")
        behaviour[Pair(oldState, currentState)]!!.invoke()
    }

    fun next(state:State) {
        go(currentState, state)
    }

    fun start() {
        next(State.START)
    }

    fun error() {
        currentState = State.ERROR
        callbacks.error()
    }
}

class AcquireCarnetHandler(val callbacks: AcquireCarnetFlow.Callbacks): AcquireCarnetFlow.Actions {
    val flow = AcquireCarnetFlow(this, this.callbacks)

    override fun activatePcdModeAction() {

    }

    override fun carnetInRangeAction() {

    }

    override fun readCarnetAction() {

    }

    override fun deactivatePcdModeAction() {

    }
}


class AcquireCarnetHelper: AcquireCarnetFlow.Callbacks {
    val handler = AcquireCarnetHandler(this)
    val flow    = AcquireCarnetFlow(handler, this)

    override fun start() {

        flow.start()
    }

    override fun predecessorNotFoundCallback() {

    }

    override fun predecessorFoundCallback() {
        
    }

    override fun completed() {

    }

    override fun error() {

    }
}