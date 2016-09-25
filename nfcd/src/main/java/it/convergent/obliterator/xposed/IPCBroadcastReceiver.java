package it.convergent.obliterator.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

public class IPCBroadcastReceiver extends BroadcastReceiver {

    public IPCBroadcastReceiver(Context ctx) {
        HandlerThread handlerThread = new HandlerThread("ht");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        ctx.registerReceiver(this, new IntentFilter("it.convergent.obliterator.daemoncall"), null, handler);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        Log.d("HOOKNFC", "Command: " + action);

        if("ENABLE".equals(action)) {
            Native.Instance.setEnabled(true);
        }
        else if("DISABLE".equals(action)) {
            Native.Instance.setEnabled(false);
        }
        else if("UPLOAD".equals(action)) {
            Native.Instance.uploadConfiguration(
                    intent.getByteExtra("atqa", (byte)0),
                    intent.getByteExtra("sak", (byte)0),
                    intent.getByteArrayExtra("hist"),
                    intent.getByteArrayExtra("uid")
            );
        }
        else if("REQSTATE".equals(action)) {
            Intent toaster = new Intent("it.convergent.obliterator.toaster");
            toaster.putExtra("text", "Patch state: " + (Native.Instance.isEnabled() ? "Active" : "Inactive"));
            context.sendBroadcast(toaster);
        }
    }
}
