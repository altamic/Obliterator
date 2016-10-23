package it.convergent.obliterator.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

/**
 * Created by altamic on 23/10/16.
 */

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
        Log.d("IPCBroadcastReceiver", "Command: " + action);

        if (action != null) {
            switch (action) {
                case "ENABLE":
                    Native.Instance.setEnabled(true);
                    break;
                case "DISABLE":
                    Native.Instance.setEnabled(false);
                    break;
                case "UPLOAD":
                    byte atqa = intent.getByteExtra("atqa", (byte)0);
                    byte sak  = intent.getByteExtra("sak", (byte)0);
                    byte[] uid  = intent.getByteArrayExtra("uid");
                    byte[] data = intent.getByteArrayExtra("data");

                    Native.Instance
                            .uploadConfiguration(atqa, sak, uid, data);
                    break;
                case "REQSTATE":
                    break;
                default:
                    break;
            }
        }
    }
}
