package it.convergent.obliterator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.os.Vibrator
import android.widget.ProgressBar
import android.widget.Toast
import it.convergent.obliterator.Maybe.None
import it.convergent.obliterator.state_machines.AcquireCarnetFlow
import it.convergent.obliterator.state_machines.AcquireCarnetHandler
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity: Activity(),  AcquireCarnetFlow.Callbacks {

    val TAG: String = this.javaClass.simpleName

    private val handler by lazy { AcquireCarnetHandler(this.baseContext, this) }
    private val flow    by lazy { AcquireCarnetFlow(handler, this) }

    //  Preferences
    val sharedPrefs: SharedPreferences by lazy {
        baseContext
            .getSharedPreferences(getString(R.string.preference_file_key),
                                    Context.MODE_PRIVATE)
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
    var isBus = true
    var originalCarnet: Maybe<Carnet>  = None
    var cookedCarnet: Maybe<Carnet> = None

    val progressBar by lazy { findViewById(R.id.progressBar) as ProgressBar }

    // interfaces
    interface OnReadyToUpdateGui {
        fun onPublishProgress(value: Int)
        fun onCompleted()
        fun onError(message: String)
    }

    interface OnDataReceived {
        fun onDataReceived(maybeCarnet: Maybe<Carnet>)
    }

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
                override fun onDataReceived(maybeCarnet: Maybe<Carnet>) {
                    originalCarnet = maybeCarnet
                }
        }
    }

    // Activity life cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onResume() {
        super.onResume()
        handler.enableForegroundDispatch(WeakReference(this))
    }

    override fun onPause() {
        super.onPause()
        handler.disableForegroundDispatch(WeakReference(this))
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent != null && handler.isNfcDiscovered(intent)) {
            val mifareUltralight = handler.getTag(intent)
             if (mifareUltralight != null)
                 readTag(mifareUltralight)
        }
    }

    private fun readTag(mifareUltralight: MifareUltralight) {
        ReadMifareUltralight(guiListener, carnetListener)
                .execute(mifareUltralight)
    }

    // flows callback
    override fun start() {

    }


    override fun predecessorNotFoundCallback() {

    }

    override fun predecessorFoundCallback() {

    }

    override fun completed() {

    }

    override fun error() {

    }

    private fun obliterateTag(mifareUltralight: MifareUltralight) {
        WriteMifareUltralight(guiListener)
                            .execute(mifareUltralight)
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
}
