package it.convergent.obliterator.nfc

/**
 * Created by altamic on 29/10/16.
 */


import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import it.convergent.obliterator.hexStringToByteArray
import it.convergent.obliterator.toHexString
import java.nio.ByteBuffer

class RawFrameService : HostApduService() {

    val TAG = javaClass.simpleName

    private val read4Blocks = 0x30.toByte() // 0x0
    private val read2Blocks = 0x31.toByte() //
    private val write1Block = 0xA2.toByte() // Valid Blocks 0x2 to 0xF
    private val compatWrite1Block = 0xA0.toByte() // Valid Blocks 0x2 to 0xF
    private val write2Blocks = 0xA1.toByte() // Valid Blocks


    private val ack = 0x0A.toByte()
    private val nack = byteArrayOf(0x0, 0x1, 0x4, 0x5).first()

    private val dataKey = "content"

    private val DONT_RESPOND = byteArrayOf()
    private val NACK = byteArrayOf(nack)

    var data: ByteArray = hexStringToByteArray(arrayOf(
            "048A8284",
            "62753380",
            "A448F203",
            "7FFFFFFF",
            "01050000",
            "020102BD",
            "59C22000",
            "00AE10A6",
            "B80044F3",
            "705BE135",
            "5A09E400",
            "04F80000",
            "5A09E400",
            "03E70004",
            "F8AE1075",
            "1012A40E")
            .joinToString(separator = ""))

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) data = intent.getByteArrayExtra(dataKey)
        Log.d(TAG, "INITIALIZED DATA: ${data.toHexString()}")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "FRAME-IN: " + apdu.toHexString())
        val firstByte = apdu.first()

        val frameOut = when (firstByte) {
            read4Blocks,
            read2Blocks -> {
                if (apdu.size == 2) {
                    val address = apdu[1]
                    readBlocks(firstByte, address)
                } else {
                    NACK
                }
            }

            write1Block,
            write2Blocks -> {
                NACK
            }

            compatWrite1Block -> {
                NACK
            }

            else -> {
                NACK
            }
        }

        Log.d(TAG, "FRAME-OUT: " + frameOut.toHexString())

        return frameOut
    }

    override fun onDeactivated(reason: Int) {
        Log.i(TAG, "Deactivated: " + reason)
    }

    fun readBlocks(commandType: Byte, address: Byte): ByteArray {
        val blockSizeInBytes = 4
        return when (commandType) {
            read4Blocks -> {
                if (address in 0x0..0xF) {
                    val duplicate = ByteBuffer
                                    .allocate(data.size * 2)
                                    .put(data).put(data)
                                    .array()

                    slice(duplicate, blockSizeInBytes * (address + 0),
                                        blockSizeInBytes * (address + 4))
                } else {
                    NACK
                }
            }

            read2Blocks -> {
                if (address in 0x0..0xF) {
                    val duplicate = ByteBuffer
                            .allocate(data.size * 2)
                            .put(data).put(data)
                            .array()
                    slice(duplicate, blockSizeInBytes * (address + 0),
                            blockSizeInBytes * (address + 1))
                } else {
                    NACK
                }
            }

            else -> NACK
        }
    }

    fun slice(source: ByteArray, srcBegin: Int, srcEnd: Int): ByteArray {
        val destination = ByteArray(srcEnd - srcBegin)
        System.arraycopy(source, srcBegin, destination, 0, srcEnd - srcBegin)
        return destination
    }
}

