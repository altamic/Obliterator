package it.convergent.obliterator.xposed;

/**
 * Created by altamic on 22/10/16.
 */

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Hooks implements IXposedHookLoadPackage {

    private NFCReceiver mReceiver;

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
//            Log.d("Obliterator", link);
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
                        Log.d("Obliterator", "Loaded lib from: " + destination);
                        break;
                    }

                    destination = String.format(path, "-" + String.valueOf(i));
                    System.load(destination);
                    Log.d("Obliterator", "Loaded lib from: " + destination);
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
                mReceiver = new NFCReceiver(app);
            }
        });


        // hook findSelectAid to route all APDUs to our app
        // and force call to notifyHostEmulationActivated (???)
        findAndHookMethod("com.android.nfc.cardemulation.HostEmulationManager", lpparam.classLoader, "findSelectAid", byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                Log.i("HOOKNFC", "beforeHookedMethod");
                if (Native.Instance.isEnabled()) {
                    Log.i("HOOKNFC", "enabled");

                    final Object THIS = param.thisObject;
                    final int STATE_XFER = 4;
                    Field mstate = XposedHelpers.findField(THIS.getClass(), "mState");
                    mstate.setInt(THIS, STATE_XFER);
                    // F0010203040506 is a aid registered by the obliterator hce service
                    param.setResult("F0010203040506");
                }
            }
        });

        // support extended length apdus
        // see http://stackoverflow.com/questions/25913480/what-are-the-requirements-for-support-of-extended-length-apdus-and-which-smartph
//        findAndHookMethod("com.android.nfc.dhimpl.NativeNfcManager", lpparam.classLoader, "getMaxTransceiveLength", int.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//
//                int technology = (int) param.args[0];
//                if (technology == 3 /* 3=TagTechnology.ISO_DEP */) {
//                    param.setResult(2462);
//                }
//            }
//        });
    }
}
