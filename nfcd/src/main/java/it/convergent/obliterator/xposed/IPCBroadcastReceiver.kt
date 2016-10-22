package it.convergent.obliterator.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

class IPCBroadcastReceiver(ctx: Context) : BroadcastReceiver() {

    init {
        val handlerThread = HandlerThread("ht")
        handlerThread.start()
        val looper = handlerThread.looper
        val handler = Handler(looper)
        ctx.registerReceiver(this,
                IntentFilter("it.convergent.obliterator.daemoncall"),
                                null, handler)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action")
        Log.d("HOOKNFC", "Command: " + action)

        if (action != null) {
            when (action) {
                "ENABLE"  -> { Native.Instance.setEnabled(true) }
                "DISABLE" -> { Native.Instance.setEnabled(false) }
                "UPLOAD"  -> {
                    val atqa = intent.getByteExtra("atqa", 0.toByte())
                    val sak  = intent.getByteExtra("sak", 0.toByte())
                    val uid  = intent.getByteArrayExtra("uid")
                    val data = intent.getByteArrayExtra("data")
                    Native.Instance
                            .uploadConfiguration(atqa, sak, uid, data)
                }
                "REQSTATE" -> {
                    val status = if (Native.Instance.isEnabled()) "Active" else "Inactive"
                    val toaster = Intent("it.convergent.obliterator.toaster")
                    toaster.putExtra("text", "Patch state: $status")
                    context.sendBroadcast(toaster)
                }
            }
        }
    }
}
