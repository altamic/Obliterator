package it.convergent.obliterator

import android.nfc.tech.NfcA
import android.os.AsyncTask
import it.convergent.obliterator.MainActivity.OnReadyToUpdateGui

/**
 * Created by altamic on 02/06/16.
 */

class WriteMifareUltralightCompatible(val listener: OnReadyToUpdateGui):
        AsyncTask<NfcA, Int, Void>() {

    override fun doInBackground(vararg params: NfcA?): Void? {
        val NfcA        = params.first()
        val currentTime = GttEpoch.currentTime().shl(bitCount = 8)

        val pageA = currentTime
        val pageB = 0x00100000
        val pageC = currentTime
        val pageD = 0x03210000
        val pageE = 0x10AE1047
        val pageF = 0x4C12282D

        publishProgress(0)

        try {
            NfcA?.connect()
            publishProgress(20)

            NfcA?.writePage(0xA, pageA.toByteArray())
            NfcA?.writePage(0xB, pageB.toByteArray())
            publishProgress(60)

            NfcA?.writePage(0xC, pageC.toByteArray())
            NfcA?.writePage(0xD, pageD.toByteArray())
            publishProgress(40)

            NfcA?.writePage(0xE, pageE.toByteArray())
            NfcA?.writePage(0xF, pageF.toByteArray())
            publishProgress(80)

        } catch(e: Exception) {
            listener.onError(e.message.toString())
        } finally { NfcA?.close() }

        publishProgress(100)

        listener.onCompleted()

        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        listener.onPublishProgress(values.first()!!)
    }
}