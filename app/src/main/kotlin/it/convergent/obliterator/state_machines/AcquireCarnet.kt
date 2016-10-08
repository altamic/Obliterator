package it.convergent.obliterator.state_machines

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import it.convergent.obliterator.R
import java.lang.ref.WeakReference

/**
 * Created by altamic on 02/10/16.
 */
class AcquireCarnetFlow(val actions: Actions, val callbacks: Callbacks) {

    val TAG: String = this.javaClass.simpleName

    // internal actions
    interface Actions {
        fun activatePcdModeAction()
        fun carnetInRangeAction()
        fun deactivatePcdModeAction()
    }

    // can be triggered externally (or trigger external behaviour)
    interface Callbacks {
        fun startAcquireCarnet()
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
            Pair(State.START,             State.WAIT_FOR_CARNET)    to { actions.activatePcdModeAction() },
            Pair(State.WAIT_FOR_CARNET,   State.CARNET_IN_RANGE)    to { actions.carnetInRangeAction() },
            Pair(State.CARNET_IN_RANGE,   State.CARNET_READ)        to { callbacks.carnetReadCallback() },
            Pair(State.CARNET_READ,       State.PREDECESSOR_FOUND)  to { callbacks.predecessorCarnetFoundCallback() },
            Pair(State.PREDECESSOR_NOT_FOUND, State.WAIT_FOR_CARNET)    to { callbacks.predecessorCarnetNotFoundCallback() },
            Pair(State.PREDECESSOR_FOUND, State.PCD_DEACTIVATED)    to { actions.deactivatePcdModeAction() },
            Pair(State.PCD_DEACTIVATED,   State.END)                to { callbacks.acquireCarnetCompleted() }
    )

    var currentState = State.START

    private fun go(current:State, next:State) {
        if (transitions[current]!!.contains(next))
            transaction(next)
        else
            error()
    }

    private fun transaction(next:State) {
        Log.d(TAG, "Trying to go from $currentState to $next")
        val oldState = currentState
        currentState = next
        val transition = behaviour[Pair(oldState, currentState)]!!
        Log.d(TAG, "invoking: $transition")
        transition.invoke()
        Log.d(TAG, "current state now is: $currentState")
    }

    private fun next(state:State) {
        go(currentState, state)
    }

    fun start() {
        next(State.START)
    }

    fun error() {
        currentState = State.ERROR
        callbacks.acquireCarnetError()
    }
}

class AcquireCarnetHandler(val context: Context, val callbacks: AcquireCarnetFlow.Callbacks): AcquireCarnetFlow.Actions {

    val flow = AcquireCarnetFlow(this, this.callbacks)

    var isPcdModeActive = false

    private val EXTRA_TAG = "android.nfc.extra.TAG"
    private val NFC_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED"

    private val mfUltralight: String = MifareUltralight::class.java.name

    private val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
    private val techlist = arrayOf(arrayOf(mfUltralight))

    val adapter: NfcAdapter by lazy {
        (this.context
                .getSystemService(Context.NFC_SERVICE)
                as NfcManager).defaultAdapter
    }

    // Pending intent
    val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(this.context, 0, Intent(this.context, this.context.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    override fun activatePcdModeAction() {
        isPcdModeActive = true
    }

    override fun deactivatePcdModeAction() {
        isPcdModeActive = false
    }

    override fun carnetInRangeAction() { }

    fun enableForegroundDispatch(weakRefActivity: WeakReference<Activity>) {
        adapter.enableForegroundDispatch(weakRefActivity.get(), pendingIntent, filters, techlist)
        activatePcdModeAction()
    }

    fun disableForegroundDispatch(weakRefActivity: WeakReference<Activity>) {
        adapter.disableForegroundDispatch(weakRefActivity.get())
        deactivatePcdModeAction()
    }

    fun isNfcDiscovered(intent: Intent): Boolean {
        val action = intent.action
        return (action == NFC_TECH_DISCOVERED)
    }

    fun getTag(intent: Intent): MifareUltralight? {
        val tag: Tag = intent.getParcelableExtra(EXTRA_TAG)
        val techList = tag.techList
        if (techList.any { tech -> tech.equals(MifareUltralight::class.java.name) }) {
            carnetInRangeAction()
            return MifareUltralight.get(tag)
        }
        return null
    }

    fun isNfcEnabled(): Boolean {
        return adapter.isEnabled
    }

    fun activateNfcRequest() {
        goToNfcSettings()
        Toast.makeText(this.context, this.context
                .getString(R.string.activate_nfc_request),
                Toast.LENGTH_LONG).show()
    }

    private fun goToNfcSettings() {
        var intent: Intent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            intent = Intent(Settings.ACTION_NFC_SETTINGS)
        } else {
            intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }

        this.context.startActivity(intent)
    }
}