package it.convergent.obliterator

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.widget.ProgressBar
import android.widget.Toast
import it.convergent.obliterator.Maybe.None
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity: Activity() {
    val TAG: String = this.javaClass.simpleName

    val NFC_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED"
    val EXTRA_TAG = "android.nfc.extra.TAG"

    val mfUltralight: String = MifareUltralight::class.java.name

    //  Preferences
    val sharedPrefs: SharedPreferences by lazy {
        baseContext
            .getSharedPreferences(getString(R.string.preference_file_key),
                                    Context.MODE_PRIVATE)
    }

    // Executor
    val singleThreadExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    // System services
    val adapter: NfcAdapter by lazy {
        (baseContext
                .getSystemService(Context.NFC_SERVICE)
                as NfcManager).defaultAdapter
    }

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

    // Pending intent
    val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(this, 0, Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }


    // Activity life cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isNfcEnabled()) {
            goToNfcSettings()
            Toast.makeText(baseContext,
                    getString(R.string.activate_nfc_request),
                    Toast.LENGTH_LONG).show()
        }

    }

    override fun onResume() {
        super.onResume()
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        val techlist = arrayOf(arrayOf(mfUltralight))
        adapter.enableForegroundDispatch(this, pendingIntent, filters, techlist)
    }

    override fun onPause() {
        super.onPause()
        adapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        val action = intent?.action
        if (action.equals(NFC_TECH_DISCOVERED))
            this.getTag(intent!!)
    }

    // private methods
    private fun getTag(intent: Intent) {
        val tag: Tag = intent.getParcelableExtra(EXTRA_TAG)

        val techList = tag.techList

        if (techList.any { tech -> tech.equals(MifareUltralight::class.java.name) }) {
            val mifareUltralight = MifareUltralight.get(tag)
            readTag(mifareUltralight)
        }
    }

    private fun readTag(mifareUltralight: MifareUltralight) {
        ReadMifareUltralight(guiListener, carnetListener)
                            .execute(mifareUltralight)
    }

    private fun obliterateTag(mifareUltralight: MifareUltralight) {
        WriteMifareUltralight(guiListener)
                            .execute(mifareUltralight)
    }

    private fun isNfcEnabled(): Boolean {
        return adapter.isEnabled
    }

    private fun goToNfcSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            intent = Intent(Settings.ACTION_NFC_SETTINGS)
        } else {
            intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }

        startActivity(intent)
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
}
