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

    private IPCReceiver mReceiver;

    @SuppressLint("SdCardPath")
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!"com.android.nfc".equals(lpparam.packageName))
            return;

        //System.load("/data/data/it.convergent.obliterator/lib/libnfc-native.so");

        String lib  = "nfc-native";
        try {
            System.loadLibrary(lib);
        } catch (UnsatisfiedLinkError e) {
            // DON'T WORK
//            String link = String.format("/data/data/it.convergent.obliterator/lib/lib%s.so", lib);
//            Log.d("IPCReceiver", link);
//
//            String target = Os.readlink(link); // EACCESS
//            System.load(target);

            // SUPER HACK
            String path = "/data/app/it.convergent.obliterator%s/lib/arm/libnfc-native.so";
            String destination;
            for (int i = 0; i < 6; i++) {
                try {
                    if (i == 0) {
                        destination = String.format(path, "");
                        System.load(destination);
                        break;
                    }

                    destination = String.format(path, "-" + String.valueOf(i));
                    System.load(destination);
                    break;

                } catch (UnsatisfiedLinkError err) {
                    Log.d("Obliterator",
                            String.format("Failed loading lib %s attempt #%s", lib, String.valueOf(i)));
                }
            }
        }

        // hook construtor to catch application context
        findAndHookConstructor("com.android.nfc.NfcService", lpparam.classLoader, Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.i("Obliterator", "constructor");
                Application app = (Application) param.args[0];
                mReceiver = new IPCReceiver(app);
            }
        });
    }
}
