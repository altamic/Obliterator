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
                    Native.Instance.uploadConfiguration(
                        intent.getByteExtra("atqa", 0.toByte()),
                        intent.getByteExtra("sak", 0.toByte()),
                        intent.getByteArrayExtra("hist"),
                        intent.getByteArrayExtra("uid"))
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
