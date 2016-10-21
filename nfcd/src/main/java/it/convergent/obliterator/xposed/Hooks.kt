package it.convergent.obliterator.xposed


import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
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

        // hook findSelectAid to route all APDUs to our app
        findAndHookMethod("com.android.nfc.cardemulation.HostEmulationManager", lpparam.classLoader, "findSelectAid", ByteArray::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam?) {

                Log.i("HOOKNFC", "beforeHookedMethod")
                if (Native.Instance.isEnabled()) {
                    Log.i("HOOKNFC", "enabled")
                    // setting a result will prevent the original method to run.
                    // F0010203040506 is a aid registered by the nfcgate hce service
                    param!!.result = "F0010203040506"
                }
            }
        })

        // support extended length apdus
        // see http://stackoverflow.com/questions/25913480/what-are-the-requirements-for-support-of-extended-length-apdus-and-which-smartph
        findAndHookMethod("com.android.nfc.dhimpl.NativeNfcManager", lpparam.classLoader, "getMaxTransceiveLength", Integer.TYPE, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam?) {

                val technology = param!!.args[0] as Int
                if (technology == 3 /* 3=TagTechnology.ISO_DEP */) {
                    param.result = 2462
                }
            }
        })
    }
}
