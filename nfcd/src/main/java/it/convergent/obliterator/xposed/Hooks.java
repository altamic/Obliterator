package it.convergent.obliterator.xposed;

/**
 * Created by altamic on 22/10/16.
 */

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;

public class Hooks implements IXposedHookLoadPackage {

    private IPCBroadcastReceiver mReceiver;

    @SuppressLint("SdCardPath")
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!"com.android.nfc".equals(lpparam.packageName))
            return;

        //System.loadLibrary("nfcgate-native");
        System.load("/data/data/it.convergent.obliterator/lib/libnfc-native.so");


        // hook construtor to catch application context
        findAndHookConstructor("com.android.nfc.NfcService", lpparam.classLoader, Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.i("HOOKNFC", "constructor");
                Application app = (Application) param.args[0];
                mReceiver = new IPCBroadcastReceiver(app);
            }
        });
    }
}
