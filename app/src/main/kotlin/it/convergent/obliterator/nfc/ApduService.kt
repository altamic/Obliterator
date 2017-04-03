package it.convergent.obliterator.nfc

/**
 * Created by altamic on 29/10/16.
 */


import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import it.convergent.obliterator.toHexString


/**
 * The ApduService class contains the logic for interaction with the Android HCE interface.
 * Here, we receive messages from the card reader and pass them on to the network interface.
 * The answer is determined by the other device and the reply is passed to the ApduService by
 * the Callback class.
 */
class ApduService: HostApduService() {

    val TAG = javaClass.simpleName

    /**
     * empty apdu byte array
     * when returned in the processCommandApdu, the hce service will not respond to the
     * reader request
     */
    private val DONT_RESPOND = byteArrayOf()

    /**
     * callback from the hce service when a apdu from a reader is received
     * @param apdu apdu data received from hce service
     * *
     * @param extras not used
     * *
     * @return apdu to answer
     */
    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {

        Log.d(TAG, "APDU-IN: " + apdu.toHexString())

        // Tell the HCE implementation to wait a moment
        return DONT_RESPOND
    }

    override fun onDeactivated(reason: Int) {
        Log.i(TAG, "Deactivated: " + reason)
    }
}
