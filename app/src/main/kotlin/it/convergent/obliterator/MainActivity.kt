package it.convergent.obliterator

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.ToggleButton
import it.convergent.obliterator.managers.Carnets
import it.convergent.obliterator.models.Carnet
import it.convergent.obliterator.state_machines.AcquireCarnetFlow
import it.convergent.obliterator.state_machines.AcquireCarnetFlow.State.*
import it.convergent.obliterator.state_machines.AcquireCarnetHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity: Activity(),  AcquireCarnetFlow.Callbacks {

    val TAG: String = this.javaClass.simpleName

    private val acquireCarnet by lazy { AcquireCarnetHandler(this.baseContext, this) }

    private val carnets by lazy { Carnets(this) }

    var isPcdModeActive = false

    private val EXTRA_TAG = "android.nfc.extra.TAG"
    private val NFC_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED"

    private val mfUltralight: String = MifareUltralight::class.java.name

    private val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
    private val techlist = arrayOf(arrayOf(mfUltralight))

    val adapter: NfcAdapter by lazy {
        (this.getSystemService(Context.NFC_SERVICE)
                as NfcManager).defaultAdapter
    }

    // Pending intent
    val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(this, 0, Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

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
                acquireCarnet.flow.start() // start over again
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                vibrator.vibrate(failVibration)
            }
        }
    }

    private val carnetListener by lazy { object: OnDataReceived {
                override fun onDataReceived(maybeCarnet: Carnet?) {
                    if (maybeCarnet == null) {
                        acquireCarnet.flow.start() // start over again
                    } else {
                        carnet = maybeCarnet
                        acquireCarnet.flow.next(CARNET_READ)
                    }
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
        if (intent != null && isNfcDiscovered(intent)) {
            acquireCarnet.flow.next(CARNET_IN_RANGE)
            val mifareUltralight = getTag(intent)
             if (mifareUltralight != null)
                 readTag(mifareUltralight)
        }
    }

    // utils
    private fun isNfcEnabled(): Boolean {
        return adapter.isEnabled
    }

    private fun activateNfcRequest() {
        goToNfcSettings()
        Toast.makeText(this, getString(R.string.activate_nfc_request),
                Toast.LENGTH_LONG).show()
    }

    private fun goToNfcSettings() {
        val intent: Intent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            intent = Intent(Settings.ACTION_NFC_SETTINGS)
        } else {
            intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }

        startActivity(intent)
    }
    private fun enableForegroundDispatchOrActivateNfcIfNeeded() {
        if (isPcdModeActive)
            if (isNfcEnabled()) {
                adapter.enableForegroundDispatch(this, pendingIntent, filters, techlist)
                activatePcdModeCallback()
            } else {
                activateNfcRequest()
            }
    }

    private fun disableForegroundDispatchIfNeeeded() {
        if (!isPcdModeActive)
            if (isNfcEnabled()) {
                adapter.disableForegroundDispatch(this)
                deactivatePcdModeCallback()
            }
    }

    fun isNfcDiscovered(intent: Intent): Boolean {
        val action = intent.action
        return (action == NFC_TECH_DISCOVERED)
    }

    fun getTag(intent: Intent): MifareUltralight? {
        val tag: Tag = intent.getParcelableExtra(EXTRA_TAG)
        val techList = tag.techList
        if (techList.any { tech -> tech.equals(MifareUltralight::class.java.name) }) {
            carnetInRangeCallback()
            return MifareUltralight.get(tag)
        }
        return null
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

    override fun activatePcdModeCallback() {
        Log.d(TAG, "activatePcdMode")
        isPcdModeActive = true
    }

    override fun deactivatePcdModeCallback() {
        Log.d(TAG, "deactivatePcdMode")
        isPcdModeActive = false
        acquireCarnet.flow.next(PCD_DEACTIVATED)
    }

    override fun carnetInRangeCallback() {
        Log.d(TAG, "carnetInRange")
    }

    override fun carnetReadCallback() {
        Log.d(TAG, "carnetRead: $carnet")
//        if (carnets.isPredecessorAvailable(carnet!!)) {
//            predecessor = carnets.predecessor(carnet!!)
//            acquireCarnet.flow.next(PREDECESSOR_FOUND)
//
//        } else {
//            acquireCarnet.flow.next(PREDECESSOR_NOT_FOUND)
//        }

        predecessor = carnet
        acquireCarnet.flow.next(PREDECESSOR_FOUND)

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
        Log.d(TAG, "acquireCarnetCompleted")
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
