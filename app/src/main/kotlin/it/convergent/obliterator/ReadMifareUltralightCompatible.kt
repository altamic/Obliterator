package it.convergent.obliterator

import android.nfc.tech.NfcA
import android.os.AsyncTask
import it.convergent.obliterator.MainActivity.OnDataReceived
import it.convergent.obliterator.MainActivity.OnReadyToUpdateGui
import it.convergent.obliterator.Maybe.Just
import it.convergent.obliterator.Maybe.None
import java.io.ByteArrayOutputStream

/**
 * Created by altamic on 02/06/16.
 */

class ReadMifareUltralightCompatible(val guiListener: OnReadyToUpdateGui, val carnetListener: OnDataReceived):
        AsyncTask<NfcA, Int, Carnet>() {

    override fun doInBackground(vararg params: NfcA?): Carnet? {
        val mifareUltralightCompatible = params.first()
        var carnet: Carnet? = null
        val outputStream = ByteArrayOutputStream()

        publishProgress(0)

        try {
            mifareUltralightCompatible?.connect()
            publishProgress(20)

            val page0to3 = mifareUltralightCompatible?.readPages(0x00)
            outputStream.write(page0to3)
            publishProgress(40)

            val page4to7 = mifareUltralightCompatible?.readPages(0x04)
            outputStream.write(page4to7)
            publishProgress(60)

            val page8to11 = mifareUltralightCompatible?.readPages(0x08)
            outputStream.write(page8to11)

            val page12to15 = mifareUltralightCompatible?.readPages(0x0C)
            outputStream.write(page12to15)
            publishProgress(80)

            carnet = Carnet(outputStream.toByteArray())

        } catch(e: Exception) {
            guiListener.onError(e.message.toString())
        } finally { mifareUltralightCompatible?.close() }

        publishProgress(100)

        guiListener.onCompleted()

        return carnet
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        guiListener.onPublishProgress(values.first()!!)
    }

    override fun onPostExecute(carnet: Carnet?) {
        super.onPostExecute(carnet)
        if (carnet == null)
            carnetListener.onDataReceived(None)
        else
            carnetListener.onDataReceived(Just(carnet))
    }
}