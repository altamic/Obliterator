package it.convergent.obliterator.nfc

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by altamic on 02/10/16.
 */

object HceMode {
    val TAG = javaClass.simpleName

    val actionName = "it.convergent.obliterator.nfcreceiver"

    private fun intentTemplate(action: String): Intent {
        val template = Intent()
        template.action = actionName

        template.putExtra("action", action)

        return template
    }

    private var context: Context? = null

    private val atqa = 0x00.toByte() //0x44.toByte()
    private val sak  = 0x00.toByte()
    private val hist  = byteArrayOf()

    fun initialize(context: Context): HceMode {
        HceMode.context = context
        return this
    }

    fun upload(uid: ByteArray, data: ByteArray) {
        if (notInitialized()) {
            showWarning()
            return
        }

        val intent = intentTemplate("UPLOAD")

        // prepare intent
        intent.putExtra("atqa", atqa)
        intent.putExtra("sak", sak)
        intent.putExtra("hist", uid)
        intent.putExtra("uid", hist)
        intent.putExtra("data", data)

        // upload configuration patch
        send(intent)
    }

    fun enable() {
        if (notInitialized()) {
            showWarning()
            return
        }

        val intent = intentTemplate("ENABLE")

        // enable patch
        send(intent)
    }

    fun disable() {
        if (notInitialized()) {
            showWarning()
            return
        }

        val intent = intentTemplate("DISABLE")

        // reset original configuration
        send(intent)
    }

    fun requestStatus() {
        if (notInitialized()) {
            showWarning()
            return
        }

        val intent = intentTemplate("REQSTATE")

        send(intent)
    }

    private fun notInitialized(): Boolean {
        return context == null
    }

    private fun send(intent: Intent) {
        context?.sendBroadcast(intent)
    }

    private fun showWarning() {
        Log.w(TAG, "Context has not been initialized!")
        Log.w(TAG, "invoke: 'initialize(context: Context)' before!")
    }
}
