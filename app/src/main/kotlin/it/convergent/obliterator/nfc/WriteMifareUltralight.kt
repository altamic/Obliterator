package it.convergent.obliterator.nfc

import android.nfc.tech.MifareUltralight
import android.os.AsyncTask
import it.convergent.obliterator.GttEpoch
import it.convergent.obliterator.MainActivity.OnReadyToUpdateGui
import it.convergent.obliterator.toByteArray

/**
 * Created by altamic on 02/06/16.
 */

class WriteMifareUltralight(val listener: OnReadyToUpdateGui):
        AsyncTask<MifareUltralight, Int, Void>() {

    override fun doInBackground(vararg params: MifareUltralight?): Void? {
        val mifareUltralight = params.first()
        val currentTime      = GttEpoch.currentTime().shl(bitCount = 8)

        val pageA = currentTime
        val pageB = 0x00100000
        val pageC = currentTime
        val pageD = 0x03210000
        val pageE = 0x10AE1047
        val pageF = 0x4C12282D

        publishProgress(0)

        try {
            mifareUltralight?.connect()
            publishProgress(20)

            mifareUltralight?.writePage(0xA, pageA.toByteArray())
//            mifareUltralight?.writePage(0xB, pageB.toByteArray())
            publishProgress(60)

            mifareUltralight?.writePage(0xC, pageC.toByteArray())
//            mifareUltralight?.writePage(0xD, pageD.toByteArray())
            publishProgress(40)

//            mifareUltralight?.writePage(0xE, pageE.toByteArray())
//            mifareUltralight?.writePage(0xF, pageF.toByteArray())
            publishProgress(80)

        } catch(e: Exception) {
            listener.onError(e.message.toString())
        } finally { mifareUltralight?.close() }

        publishProgress(100)

        listener.onCompleted()

        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        listener.onPublishProgress(values.first()!!)
    }
}