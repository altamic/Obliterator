package it.convergent.obliterator

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import it.convergent.obliterator.Maybe.Just
import it.convergent.obliterator.Maybe.None
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import javax.net.ssl.HttpsURLConnection

open class MainActivity: Activity() {
    val TAG: String = this.javaClass.simpleName

    val NFC_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED"
    val EXTRA_TAG = "android.nfc.extra.TAG"

    val nfcA: String = NfcA::class.java.name

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
    var currentCarnet: Maybe<Carnet>  = None
    var previousCarnet: Maybe<Carnet> = None

    //  UI
    val gttTimeHex by lazy { findViewById(R.id.gttTimeHex) as TextView }
    val gttTimeBinary by lazy { findViewById(R.id.gttTimeBinary) as TextView }
    val dateTime by lazy { findViewById(R.id.dateTime) as TextView }
    val toggleChangeDateTime by lazy { findViewById(R.id.toggleChangeDateTime) as ToggleButton }
    val decrementDateTime by lazy { findViewById(R.id.decrementDateTime) as Button }
    val incrementDateTime by lazy { findViewById(R.id.incrementDateTime) as Button }
    val carnetRead  by lazy { findViewById(R.id.carnetRead) as TextView }
    val progressBar by lazy { findViewById(R.id.progressBar) as ProgressBar }

    val dateTimeFormatter by lazy { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ITALIAN) }

    // Carnet data queue to be sent remotely
    val pendingCarnetsToBeSent by lazy { LinkedBlockingQueue<Carnet>() }

    // res
    val carnetJsonSetKey: String by lazy { getString(R.string.json_carnet_set_key) }

    // interfaces
    interface OnReadyToUpdateGui {
        fun onPublishProgress(value: Int)
        fun onCompleted()
        fun onError(message: String)
    }

    interface OnDataReceived {
        fun onDataReceived(maybeCarnet: Maybe<Carnet>)
    }

    interface OnCalendarUpdated {
        fun onCalendarUpdated(calendar: Calendar?)
        fun onCalendarUpdateError(e: Throwable?)
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
                    previousCarnet = currentCarnet
                    currentCarnet = maybeCarnet

                    if (maybeCarnet is Just<Carnet> &&
                            !pendingCarnetsToBeSent.contains(maybeCarnet.value))
                        pendingCarnetsToBeSent.add(maybeCarnet.value)

                    showCarnet()
                }
        }
    }

    private val calendarListener by lazy { object: OnCalendarUpdated {
            override fun onCalendarUpdated(calendar: Calendar?) {
                runOnUiThread {
                    currentCalendar = calendar!!
                    updateGttTimeBinary()
                    updateGttTimeHex()
                    updateDateTime()
                }
            }

            override fun onCalendarUpdateError(e: Throwable?) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, e!!.message, Toast.LENGTH_LONG)
                            .show()
                }
            }
        }
    }

    // Pending intent
    val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(this, 0, Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    // rx
    var currentCalendar: Calendar = Calendar.getInstance(Locale.ITALIAN)
    var calendarSubscriber = flow.calendarSubscriber(calendarListener)

    // Activity life cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pendingCarnetsToBeSent.addAll(loadCarnetSetToBeSent())

        if (!isNfcEnabled()) {
            goToNfcSettings()
            Toast.makeText(baseContext,
                        getString(R.string.activate_nfc_request),
                        Toast.LENGTH_LONG).show()
        }

        flow.generateFrom(currentCalendar)
                .subscribe(calendarSubscriber)

        toggleChangeDateTime.setOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                decrementDateTime.isEnabled = true
                incrementDateTime.isEnabled = true
            } else {
                decrementDateTime.isEnabled = false
                incrementDateTime.isEnabled = false
            }
        }

        decrementDateTime.setOnClickListener { minusOneMinute() }
        incrementDateTime.setOnClickListener { plusOneMinute() }
    }

    override fun onDestroy() {
        calendarSubscriber.unsubscribe()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        val techlist = arrayOf(arrayOf(nfcA))
        adapter.enableForegroundDispatch(this, pendingIntent, filters, techlist)
    }

    override fun onPause() {
        super.onPause()
        adapter.disableForegroundDispatch(this)

        persistCarnetsToBeSent()

        if (isNetworkAvailable() && !pendingCarnetsToBeSent.isEmpty()) {
            singleThreadExecutor
                    .execute {
                        httpPostJson(pendingCarnetsToBeSent)
                    }
        }
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

        if (techList.any { tech -> tech.equals(nfcA) }) {
            val nfcA = NfcA.get(tag)
            val atqa = nfcA.atqa.asList()
            if (atqa.first().equals(0x44.toByte()) &&
                    atqa.last().equals(0x00.toByte()) &&
                    nfcA.sak == 0x00.toShort())
                readTag(nfcA)
        }
    }

    private fun readTag(mifareUltralightCompatible: NfcA) {
        ReadMifareUltralightCompatible(guiListener, carnetListener)
                            .execute(mifareUltralightCompatible)
    }

    private fun obliterateTag(mifareUltralightCompatible: NfcA) {
        WriteMifareUltralightCompatible(guiListener)
                            .execute(mifareUltralightCompatible)
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

    fun persistCarnetsToBeSent() {
        val jsonCarnetSet = pendingCarnetsToBeSent.toSet().toJson()

        sharedPrefs.edit()
                    .putString(carnetJsonSetKey, jsonCarnetSet)
                    .apply()
    }

    fun loadCarnetSetToBeSent(): Set<Carnet> {
        val jsonEmptyArrayDefault = "[]"

        val jsonCarnetList = sharedPrefs
                .getString(carnetJsonSetKey, jsonEmptyArrayDefault)

        return jsonCarnetList.toCarnetSet()
    }

    fun httpPostJson(carnetsToBeSent: LinkedBlockingQueue<Carnet>) {
        val readTimeout    = 15 * 1000 // milliseconds
        val connectTimeout = 20 * 1000 // milliseconds

        val message = carnetsToBeSent.toSet().toJson()

        //constants
        val url = URL(BuildConfig.POST_CARNET_COLLECTION_API)

        var conn: HttpURLConnection?  = null
        var os: BufferedOutputStream? = null
        val iStream: InputStream?     = null

        try {
            conn = url.openConnection() as HttpsURLConnection
            conn.readTimeout = readTimeout
            conn.connectTimeout = connectTimeout

            conn.setRequestProperty(ebgGuvegrra("abvgnmvebughN").reversed(), ebgGuvegrra("Onfvp LJ5xpz9cMQb1ZQD2AwL0MwD3Amt2LwywAzZlAwuzLzAuAmEzZwL2BN=="))
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(message.toByteArray().size)

            //make some HTTP header nicety
            conn.setRequestProperty("Content-Type", "application/jsoncharset=utf-8")
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")

            //open
            conn.connect()

            //setup send
            os = BufferedOutputStream(conn.outputStream)
            os.write(message.toByteArray())

            //clean up
            os.flush()

            val responseCode = conn.responseCode

            when (responseCode) {
                HttpsURLConnection.HTTP_OK,
                HttpsURLConnection.HTTP_CREATED,
                HttpsURLConnection.HTTP_ACCEPTED,
                HttpsURLConnection.HTTP_NO_CONTENT -> {
                    // we can remove pending carnets
                    carnetsToBeSent.removeAll(carnetsToBeSent)
                    Log.i(TAG, "TX OK: $message")
                    carnetSentSuccessToast()
                }

                HttpsURLConnection.HTTP_NOT_MODIFIED -> {
                    // we can remove pending carnets
                    carnetsToBeSent.removeAll(carnetsToBeSent)
                    Log.i(TAG, "TX OK: $message\n" +
                            "HTTP Status: $responseCode")
                    carnetSentSuccessToast()
                }

                else -> {
                    Log.w(TAG, "TX KO: $message\n" +
                            "HTTP Status: $responseCode")
                    carnetsSentFailureToast()
                }
            }
        } catch(e: Exception) {
            val exception = e
            Log.w(TAG, "TX KO: $message\nException: ${exception.message}")
            carnetsSentFailureToast()
        } finally {
            //clean up
            os?.close()
            iStream?.close()
            conn?.disconnect()
            runOnUiThread { persistCarnetsToBeSent() }
        }
    }

    private fun updateGttTimeBinary() {
        runOnUiThread {
            val gttTime = GttEpoch.currentTime(currentCalendar)
            val binaryString = Integer.toBinaryString(gttTime)
            val paddedBinaryString = String.format("%1$24s", binaryString).replace(' ', '0')

            gttTimeBinary.text = paddedBinaryString

            val baseList = listOf(0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
                            .map { Integer.toBinaryString(it) }
                            .map { String.format("%1$7s", it).replace(' ', '0') }

            val compoundList = listOf(0x03, 0x05, 0x07, 0x0D, 0x14, 0x1C,
                                0x2A, 0x38, 0x50, 0x58, 0x5A)
                                .map { Integer.toBinaryString(it) }
                                .map { String.format("%1$7s", it).replace(' ', '0') }

            if (baseList.any { paddedBinaryString.endsWith(suffix = it) })
                gttTimeBinary.setBackgroundColor(Color.GREEN)
            else if (compoundList.any { paddedBinaryString.endsWith(suffix = it) })
                gttTimeBinary.setBackgroundColor(Color.YELLOW)
            else
                gttTimeBinary.setBackgroundColor(Color.RED)
        }
    }

    private fun updateGttTimeHex() {
        runOnUiThread {
            val gttTime = GttEpoch.currentTime(currentCalendar)
            val hexString = gttTime.toByteArray()
                                   .takeLast(3)
                                   .toByteArray()
                                   .toHexString()

            gttTimeHex.text = hexString
        }
    }

    private fun updateDateTime() {
        val currentDateTime = dateTimeFormatter.format(currentCalendar.time)
        dateTime.text = currentDateTime
    }

    private fun minusOneMinute() {
        calendarSubscriber.unsubscribe()
        calendarSubscriber = flow.calendarSubscriber(calendarListener)

        currentCalendar.add(Calendar.MINUTE , -1)

        flow.generateFrom(currentCalendar)
                .subscribe(calendarSubscriber)
    }

    private fun plusOneMinute() {
        calendarSubscriber.unsubscribe()
        calendarSubscriber = flow.calendarSubscriber(calendarListener)

        currentCalendar.add(Calendar.MINUTE , 1)

        flow.generateFrom(currentCalendar)
                .subscribe(calendarSubscriber)
    }

    private fun carnetsSentFailureToast() {
        runOnUiThread {
            Toast.makeText(this@MainActivity, R.string.carnets_sent_failure,
                    Toast.LENGTH_LONG).show()
        }
    }

    private fun carnetSentSuccessToast() {
        runOnUiThread {
            Toast.makeText(this@MainActivity, R.string.carnets_sent_success,
                    Toast.LENGTH_LONG).show()
        }
    }

    private fun showCarnet() {
        when (currentCarnet) {
            is Just<Carnet> -> {
                val carnetStringData = (currentCarnet as Just<Carnet>).value.toString()
                val splitEvery8chars = "([A-Za-z0-9]{8})".toRegex()

                val humanCarnet = splitEvery8chars
                                    .findAll(carnetStringData)
                                    .map { it.value }
                                    .joinToString("\n")

                carnetRead.text = humanCarnet
                carnetRead.visibility = View.VISIBLE
            }

            is None -> { carnetRead.visibility = View.INVISIBLE }
        }
    }
}
