package it.convergent.obliterator.xposed


import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class Hooks : IXposedHookLoadPackage {

    private var mReceiver: IPCBroadcastReceiver? = null

    @SuppressLint("SdCardPath")
    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if ("com.android.nfc" != lpparam.packageName)
            return

        //System.loadLibrary("nfc-native");
        System.load("/data/data/it.convergent.obliterator/lib/libnfc-native.so")

        // hook construtor to catch application context
        findAndHookConstructor("com.android.nfc.NfcService", lpparam.classLoader, Application::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                Log.i("HOOKNFC", "constructor")
                val app = param!!.args[0] as Application
                mReceiver = IPCBroadcastReceiver(app)
            }
        })
    }
}
