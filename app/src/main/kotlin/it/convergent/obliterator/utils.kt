package it.convergent.obliterator

import android.nfc.tech.NfcA
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by altamic on 08/04/16.
 */

sealed class Maybe<out T> {
    object None: Maybe<Nothing>()
//    object Unknown: Maybe<Nothing>()
//    object Invalid: Maybe<Nothing>()
    class Just<T>(val value: T): Maybe<T>()
}

inline fun Int.times(block: (index: Int) -> Unit): Int {
    if (this > 0) {
        for (item: Int in 0.rangeTo(this.minus(1))) {
            block.invoke(item)
        }
    }
    return this
}

fun Int.isEven(): Boolean {
    return this % 2 == 0
}

fun Int.isOdd(): Boolean {
    return this % 2 == 1
}

fun Int.toByteArray(): ByteArray {
    val byteArray = ByteArray(size = 4)
    val byteBits  = 8
    val mask      = 0x000000FF

    byteArray.size.times { index ->
        val offset = byteBits * byteArray.size.minus(1).minus(index)
        byteArray[index] = this.ushr(bitCount = offset).and(mask).toByte()
    }

    return byteArray
}

fun Long.toByteArray(): ByteArray {
    val byteArray = ByteArray(size = 8)
    val byteBits  = 8
    val mask      = 0x000000FF

    byteArray.size.times { index ->
        val offset = byteBits * byteArray.size.minus(1).minus(index)
        byteArray[index] = this.ushr(bitCount = offset).and(mask.toLong()).toByte()
    }

    return byteArray
}

fun ByteArray.getShort(index: Int): Int {
    val byte1Mask = 0x000000FF
    val byte0Mask = 0x0000FF00

    val byte1 = this[index + 1].toInt().shl(bitCount =  0)
    val byte0 = this[index + 0].toInt().shl(bitCount =  8)

    return ((byte0 and byte0Mask) or (byte1 and byte1Mask))
}

fun ByteArray.getInt(index: Int): Long {
    val byte3Mask: Long = 0x000000FF
    val byte2Mask: Long = 0x0000FF00
    val byte1Mask: Long = 0x00FF0000
    val byte0Mask: Long = 0xFF000000

    val byte3: Long = this[index + 3].toLong().shl(bitCount =  0)
    val byte2: Long = this[index + 2].toLong().shl(bitCount =  8)
    val byte1: Long = this[index + 1].toLong().shl(bitCount = 16)
    val byte0: Long = this[index + 0].toLong().shl(bitCount = 24)

    return ((byte0 and byte0Mask) or
            (byte1 and byte1Mask ) or
             (byte2 and byte2Mask ) or
              (byte3 and byte3Mask))
}

fun ByteArray.toHexString(): String {
    val hexArray = "0123456789ABCDEF".toCharArray()
    val hexChars = CharArray(size = this.size * 2)

    this.size.toInt().times { index ->
        val int = this[index].toInt() and 0xFF
        hexChars[index * 2 + 0] = hexArray[int shr 4]
        hexChars[index * 2 + 1] = hexArray[int and 0x0F]
    }

    return StringBuilder()
            .append(hexChars)
            .toString()
}

fun hexStringToByteArray(hex: String): ByteArray {
    val size = hex.length

    if (size.isOdd()) {
        throw IllegalArgumentException("Hex string must have even number of characters")
    }

    if (!hex.matches("[0-9a-fA-F]+".toRegex())) {
        throw IllegalArgumentException("Hex string must have only digits " +
                                        "or case insensitive " +
                                        "letters from 'A' until 'F'")
    }

    val data = ByteArray(size / 2)

    size.times { index ->
        if (index.isEven()) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[index / 2] = (((Character.digit(hex.codePointAt(index + 0), 16)) shl 4)
                                + Character.digit(hex.codePointAt(index + 1), 16)).toByte()
        }
    }

    return data
}

fun Set<Carnet>.toJson(): String {
    return this.map { carnet -> "\"${carnet.toString()}\"" }
            .joinToString(separator = ", ", prefix = "[", postfix = "]")
}

fun String.toCarnetSet(): Set<Carnet> {
    val regex = "\\A\\[\\s*((\"[0-9A-F]{128}\")(,\\s(\"[0-9A-F]{128}\"))*)+\\]\\z".toRegex()

    return if (this.matches(regex)) {
        this.removeSurrounding(prefix = "[", suffix = "]")
                .split(", ")
                .map { it.removeSurrounding(delimiter = "\"")}
                .map { Carnet(hexStringToByteArray(it))}
                .toSet()
    } else {
        emptySet<Carnet>()
    }
}

fun ebgGuvegrra(input: String): String {
    val sb = StringBuilder()
    input.length.times { index ->
        var c: Char = input[index]
        if       (c >= 'a' && c <= 'm') c += 13
        else if  (c >= 'A' && c <= 'M') c += 13
        else if  (c >= 'n' && c <= 'z') c -= 13
        else if  (c >= 'N' && c <= 'Z') c -= 13
        sb.append(c)
    }
    return sb.toString()
}

fun NfcA.readPages(pageOffset: Int): ByteArray {
    val cmd = byteArrayOf(0x30, pageOffset.toByte())

    return transceive(cmd)
}

fun NfcA.writePage(pageOffset: Int, data: ByteArray) {
    val cmd = ByteArray(data.size + 2)
    cmd[0] = 0xA2.toByte()
    cmd[1] = pageOffset.toByte()
    System.arraycopy(data, 0, cmd, 2, data.size)

    transceive(cmd)
}


object GttEpoch {
    fun calendar(minutesSinceGttEpoch: Int): Calendar {
        val GTT_EPOCH = "01/01/2005 00:00"
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val calendar = Calendar.getInstance()

        calendar.time = formatter.parse(GTT_EPOCH)
        calendar.add(Calendar.MINUTE, minutesSinceGttEpoch)

        return calendar
    }

    fun currentTime(fromCalendar: Calendar = Calendar.getInstance()): Int {
        val millisToMinutesFactor = (1000 * 60)
        val gttCalendar = calendar(minutesSinceGttEpoch = 0) // gtt epoch

        val minutesSinceGttEpoch = ((fromCalendar.timeInMillis -
                gttCalendar.timeInMillis) / millisToMinutesFactor).toInt()

        return minutesSinceGttEpoch
    }
}