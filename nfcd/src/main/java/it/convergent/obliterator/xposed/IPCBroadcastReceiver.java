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
        HandlerThread handlerThread = new HandlerThread("ObliteratorHandlerThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        ctx.registerReceiver(this, new IntentFilter("it.convergent.obliterator.daemoncall"), null, handler);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        Log.d("Obliterator", "Command: " + action);

        if (action != null) {
            switch (action) {
                case "UPLOAD":
                    byte atqa = intent.getByteExtra("atqa", (byte) 0);
                    byte sak = intent.getByteExtra("sak", (byte) 0);
                    byte[] hist = intent.getByteArrayExtra("hist");
                    byte[] uid = intent.getByteArrayExtra("uid");
                    byte[] data = intent.getByteArrayExtra("data");

                    Log.d("Obliterator", "UPLOAD: " +
                            String.format("atqa: 0x%02X, sak: 0x%02X,\n", atqa, sak) +
                            String.format("hist: 0x%s," +
                                            "uid: 0x%s,\n" +
                                             "data: 0x%s ", bytesToHexString(hist),
                                                              bytesToHexString(uid),
                                                                bytesToHexString(data)));

                    Native.Instance
                            .uploadConfiguration(atqa, sak, uid, hist, data);
                    break;

                case "ENABLE":
                    Native.Instance.setEnabled(true);
                    break;

                case "DISABLE":
                    Native.Instance.setEnabled(false);
                    break;

                case "REQSTATE":
                    String status;
                    if (Native.Instance.isEnabled())
                        status = "Active";
                    else
                        status = "Inactive";

                    Intent toaster = new Intent("it.convergent.obliterator.toaster");
                    toaster.putExtra("text", "Patch state: " + status);
                    context.sendBroadcast(toaster);
                    break;

                default:
                    break;
            }
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (byte aByte: bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
