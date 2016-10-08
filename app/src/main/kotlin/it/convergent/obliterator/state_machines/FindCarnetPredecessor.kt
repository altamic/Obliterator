package it.convergent.obliterator.state_machines

import it.convergent.obliterator.models.Carnet

/**
 * Created by altamic on 02/10/16.
 */
class FindCarnetPredecessorFlow(val carnet: Carnet, val actions: Actions, val callbacks: Callbacks) {

    var predecessor = carnet

    // internal actions
    interface Actions {
        fun check(carnet: Carnet)
        fun isNewCarnetAction()
        fun isUsedCarnetAction()
        fun returnSameCarnetAction()
        fun lookupForPredecessorSuccess()
        fun lookupForPredecessorFailure()
    }

    // can be triggered externally (or trigger external behaviour)
    interface Callbacks {
        fun start()
        fun predecessorFoundCallback(predecessor: Carnet)
        fun predecessorNotFoundCallback()
        fun error()
    }

    enum class State {
        START,
        CHECK_CARNET,
        NEW_CARNET,
        USED_CARNET,
        PREDECESSOR_FOUND,
        PREDECESSOR_NOT_FOUND,
        END,
        ERROR
    }

    val transitions = mapOf(
            State.START to arrayOf(State.CHECK_CARNET),
            State.CHECK_CARNET to arrayOf(State.NEW_CARNET, State.USED_CARNET),
            State.NEW_CARNET to arrayOf(State.PREDECESSOR_FOUND),
            State.USED_CARNET to arrayOf(State.PREDECESSOR_FOUND, State.PREDECESSOR_NOT_FOUND),
            State.PREDECESSOR_FOUND to arrayOf(State.END),
            State.PREDECESSOR_NOT_FOUND to arrayOf(State.END)
    )

    private val behaviour = mapOf(
            Pair(State.START, State.CHECK_CARNET) to { actions.check(carnet) },
            Pair(State.CHECK_CARNET, State.NEW_CARNET) to { actions.isNewCarnetAction() },
            Pair(State.NEW_CARNET, State.PREDECESSOR_FOUND) to { actions.returnSameCarnetAction() },
            Pair(State.CHECK_CARNET, State.USED_CARNET) to { actions.isUsedCarnetAction() },
            Pair(State.USED_CARNET, State.PREDECESSOR_FOUND) to { actions.lookupForPredecessorSuccess() },
            Pair(State.USED_CARNET, State.PREDECESSOR_NOT_FOUND) to { actions.lookupForPredecessorFailure() },
            Pair(State.PREDECESSOR_FOUND, State.END) to { callbacks.predecessorFoundCallback(predecessor) },
            Pair(State.PREDECESSOR_NOT_FOUND, State.END) to { callbacks.predecessorNotFoundCallback() }
    )

    var currentState = State.START

    private fun go(current: State, next: State) {
        if (transitions[current]!!.contains(next))
            transaction(next)
        else
            error()
    }

    private fun transaction(next: State) {
        println("Trying to go from $currentState to $next")
        val oldState = currentState
        currentState = next
        println("current state: $currentState")
        behaviour[Pair(oldState, currentState)]!!.invoke()
    }

    fun next(state: State) {
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