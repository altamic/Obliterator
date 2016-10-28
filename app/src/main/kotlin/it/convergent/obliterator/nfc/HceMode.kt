package it.convergent.obliterator.nfc

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by altamic on 02/10/16.
 */

object HceMode {
    val TAG = javaClass.simpleName

    val actionName = "it.convergent.obliterator.daemoncall"

    private var context: Context? = null

    private val atqa = 0x44.toByte()
    private val sak  = 0x00.toByte()

    fun initialize(context: Context): HceMode {
        HceMode.context = context
        return this
    }

    fun upload(uid: ByteArray, data: ByteArray) {
        if (notInitialized()) {
            showWarning()
            return
        }

        // prepare intent
        val intent = Intent()
        intent.putExtra("action", "UPLOAD")
        intent.putExtra("atqa", atqa)
        intent.putExtra("sak", sak)
        intent.putExtra("hist", byteArrayOf())
        intent.putExtra("uid", uid)
        intent.putExtra("data", data)

        // upload configuration patch
        setActionAnSend(intent)
    }

    fun enable() {
        if (notInitialized()) {
            showWarning()
            return
        }

        // enable patch
        setExtraActionAndSend("ENABLE")
    }

    fun disable() {
        if (notInitialized()) {
            showWarning()
            return
        }

        // reset original configuration
        setExtraActionAndSend("DISABLE")
    }

    fun requestStatus() {
        setExtraActionAndSend("REQSTATE")
    }

    private fun notInitialized(): Boolean {
        return context == null
    }

    private fun setExtraActionAndSend(extraAction: String) {
        val intent = Intent()
        intent.putExtra("action", extraAction)
        setActionAnSend(intent)
    }

    private fun setActionAnSend(intent: Intent) {
        intent.action = actionName
        context?.sendBroadcast(intent)
    }

    private fun showWarning() {
        Log.w(TAG, "Context has not been initialized!")
        Log.w(TAG, "invoke: 'initialize(context: Context)' before!")
    }
}