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
import it.convergent.obliterator.state_machines.AcquireTagFlow
import it.convergent.obliterator.state_machines.AcquireTagFlow.State.*
import it.convergent.obliterator.state_machines.AcquireTagHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity: Activity(),  AcquireTagFlow.Callbacks {

    val TAG: String = this.javaClass.simpleName

    private val acquireCarnet by lazy { AcquireTagHandler(this.baseContext, this) }

    private val carnets by lazy { Carnets(this) }

    var isTagPollingActive = true

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
                startAcquireCarnet()   // start over again
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                vibrator.vibrate(failVibration)
            }
        }
    }

    private val carnetListener by lazy { object: OnDataReceived {
                override fun onDataReceived(maybeCarnet: Carnet?) {
                    if (maybeCarnet == null) {
                        startAcquireCarnet()  // start over again
                    } else {
                        carnet = maybeCarnet
                        acquireCarnet.flow.next(TAG_READ)
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
        if (isTagPollingActive)
            if (intent != null && isNfcDiscovered(intent)) {
                val mifareUltralight = getTag(intent)
                if (mifareUltralight != null) {
                    acquireCarnet.next(TAG_IN_RANGE)
                    readTag(mifareUltralight)
                }
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
        if (isTagPollingActive)
            if (isNfcEnabled()) {
                adapter.enableForegroundDispatch(this, pendingIntent, filters, techlist)
            } else {
                activateNfcRequest()
            }
    }

    private fun disableForegroundDispatchIfNeeeded() {
        if (!isTagPollingActive)
            if (isNfcEnabled()) {
                adapter.disableForegroundDispatch(this)
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
    override fun startAcquireCarnet() { acquireCarnet.start() }

    override fun activateTagPollingCallback() {
        isTagPollingActive = true
        Log.d(TAG, "activatePcdMode")
    }

    override fun deactivateTagPollingCallback() {
        if (isTagPollingActive) {
            Log.d(TAG, "deactivatePcdMode")
            acquireCarnet.next(END)
        }
        
        isTagPollingActive = false
    }

    override fun tagInRangeCallback() {
        Log.d(TAG, "carnetInRange")
    }

    override fun tagReadCallback() {
        Log.d(TAG, "carnetRead: $carnet")
//        if (carnets.isPredecessorAvailable(carnet!!)) {
              //flash green light led 3 of times
//            predecessor = carnets.predecessor(carnet!!)
//            acquireTagCompleted()
//            acquireCarnet.next(DEACTIVATE_POLLING)
//        } else {
//            vibrator.vibrate(failVibration)
//           // flash red light led 3 times
//            acquireTagError()
//            startAcquireCarnet() // start over again
//        }

        predecessor = carnet
        acquireCarnet.next(DEACTIVATE_POLLING)
    }

    override fun acquireTagCompleted() {
        Log.d(TAG, "acquireTagCompleted")
//        showCarnetLayout()
//        startHceMode(predecessor)
    }

    override fun acquireTagError() {
        Toast.makeText(this@MainActivity,
                            R.string.acquire_carnet_error,
                            Toast.LENGTH_LONG).show()
        vibrator.vibrate(failVibration)
    }
}
