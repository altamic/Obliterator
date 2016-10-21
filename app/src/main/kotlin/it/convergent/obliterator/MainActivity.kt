package it.convergent.obliterator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.os.Vibrator
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.ToggleButton
import it.convergent.obliterator.managers.Carnets
import it.convergent.obliterator.models.Carnet
import it.convergent.obliterator.state_machines.AcquireCarnetFlow
import it.convergent.obliterator.state_machines.AcquireCarnetHandler
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity: Activity(),  AcquireCarnetFlow.Callbacks {

    val TAG: String = this.javaClass.simpleName

    private val acquireCarnet by lazy { AcquireCarnetHandler(this.baseContext, this) }

    private val carnets by lazy { Carnets(this) }

    // Executor
    val singleThreadExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }


    val connectivityManager by lazy {
        baseContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val vibrator by lazy {
        baseContext
                .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    val successVibration: Long = 250
    val failVibration: Long    =  50

    // Models
    var carnet: Carnet?  = null
    var predecessor: Carnet?  = null

    // interfaces
    interface OnReadyToUpdateGui {
        fun onPublishProgress(value: Int)
        fun onCompleted()
        fun onError(message: String)
    }

    interface OnDataReceived {
        fun onDataReceived(maybeCarnet: Carnet?)
    }

    val progressBar by lazy { findViewById(R.id.progressBar) as ProgressBar }
    val toggleCloneOrReader by lazy { findViewById(R.id.cloneMode) as ToggleButton }

    // Listeners
    private val guiListener by lazy { object: OnReadyToUpdateGui {
            override fun onPublishProgress(value: Int) {
                progressBar.progress = value
            }

            override fun onCompleted() {
                progressBar.progress = 0
                vibrator.vibrate(successVibration)
            }

            override fun onError(message: String) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                vibrator.vibrate(failVibration)
            }
        }
    }

    private val carnetListener by lazy { object: OnDataReceived {
                override fun onDataReceived(maybeCarnet: Carnet?) {
                    carnet = maybeCarnet
                    carnetReadCallback()
                }
        }
    }

    // Activity life cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startAcquireCarnet()
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatchOrActivateNfcIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatchIfNeeeded()
    }

    override fun onDestroy() {
        carnets.persist()
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent != null && acquireCarnet.isNfcDiscovered(intent)) {
            val mifareUltralight = acquireCarnet.getTag(intent)
             if (mifareUltralight != null)
                 readTag(mifareUltralight)
        }
    }

    // utils
    private fun enableForegroundDispatchOrActivateNfcIfNeeded() {
        if (acquireCarnet.isPcdModeActive)
            if (acquireCarnet.isNfcEnabled())
                acquireCarnet.enableForegroundDispatch(WeakReference(this))
            else
                acquireCarnet.activateNfcRequest()
    }

    private fun disableForegroundDispatchIfNeeeded() {
        if (!acquireCarnet.isPcdModeActive)
            if (acquireCarnet.isNfcEnabled())
                acquireCarnet.disableForegroundDispatch(WeakReference(this))
    }

    private fun readTag(mifareUltralight: MifareUltralight) {
        ReadMifareUltralight(guiListener, carnetListener)
                .execute(mifareUltralight)
    }

    private fun obliterateTag(mifareUltralight: MifareUltralight) {
        WriteMifareUltralight(guiListener)
                .execute(mifareUltralight)
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    // flows callback
    override fun startAcquireCarnet() { acquireCarnet.flow.start() }

    override fun carnetReadCallback() {
//        if (carnet != null && carnets.isPredecessorAvailable(carnet!!)) {
//            predecessor = carnets.predecessor(carnet!!)
//            predecessorCarnetFoundCallback()
//
//        } else {
//            predecessorCarnetNotFoundCallback()
//        }

        predecessor = carnet
        predecessorCarnetFoundCallback()
    }

    override fun predecessorCarnetNotFoundCallback() {
        Toast.makeText(this@MainActivity,
                R.string.carnet_predecessor_not_found,
                Toast.LENGTH_LONG).show()
//        vibrator.vibrate(failVibration)
//        flash red light led 3 times
    }

    override fun predecessorCarnetFoundCallback() {
//        flash green light led 3 of times
    }

    override fun acquireCarnetCompleted() {
//        showCarnetLayout()
//        startHceMode(predecessor)
    }

    override fun acquireCarnetError() {
        Toast.makeText(this@MainActivity,
                            R.string.acquire_carnet_error,
                            Toast.LENGTH_LONG).show()
        vibrator.vibrate(failVibration)
    }
}
