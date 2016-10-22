package it.convergent.obliterator

import android.nfc.tech.MifareUltralight
import android.os.AsyncTask
import it.convergent.obliterator.MainActivity.OnDataReceived
import it.convergent.obliterator.MainActivity.OnReadyToUpdateGui
import it.convergent.obliterator.models.Carnet
import java.io.ByteArrayOutputStream

/**
 * Created by altamic on 02/06/16.
 */

class ReadMifareUltralight(val guiListener: OnReadyToUpdateGui, val carnetListener: OnDataReceived):
        AsyncTask<MifareUltralight, Int, ByteArray>() {

    override fun doInBackground(vararg params: MifareUltralight?): ByteArray? {
        val mifareUltralight = params.first()
        var data: ByteArray? = null
        val outputStream = ByteArrayOutputStream()

        publishProgress(0)

        try {
            mifareUltralight?.connect()
            publishProgress(20)

            val page0to3 = mifareUltralight?.readPages(0)
            outputStream.write(page0to3)
            publishProgress(40)

            val page4to7 = mifareUltralight?.readPages(4)
            outputStream.write(page4to7)
            publishProgress(60)

            val page8to11 = mifareUltralight?.readPages(8)
            outputStream.write(page8to11)

            val page12to15 = mifareUltralight?.readPages(12)
            outputStream.write(page12to15)
            publishProgress(80)

            data = outputStream.toByteArray()

        } catch(e: Exception) {
            guiListener.onError(e.message.toString())
        } finally { mifareUltralight?.close() }

        publishProgress(100)

        guiListener.onCompleted()

        return data
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        guiListener.onPublishProgress(values.first()!!)
    }

    override fun onPostExecute(data: ByteArray?) {
        super.onPostExecute(data)
        carnetListener.onDataReceived(data)
    }
}