package it.convergent.obliterator

import android.nfc.tech.MifareUltralight
import android.os.AsyncTask
import java.io.ByteArrayOutputStream
import it.convergent.obliterator.MainActivity.OnReadyToUpdateGui
import it.convergent.obliterator.MainActivity.OnDataReceived
import it.convergent.obliterator.Maybe.Just
import it.convergent.obliterator.Maybe.None

/**
 * Created by altamic on 02/06/16.
 */
class ReadMifareUltralight(guiListener: OnReadyToUpdateGui, carnetListener: OnDataReceived):
        AsyncTask<MifareUltralight, Int, Carnet>() {

    val guiListener = guiListener
    val carnetListener = carnetListener


    override fun doInBackground(vararg params: MifareUltralight?): Carnet? {
        val mifareUltralight = params.first()
        var carnet: Carnet? = null
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

            carnet = Carnet(outputStream.toByteArray())

        } catch(e: Exception) {
            guiListener.onError(e.message.toString())
        } finally { mifareUltralight?.close() }

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