package it.convergent.obliterator

import it.convergent.obliterator.Maybe.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by altamic on 08/04/16.
 */

class Carnet(data: ByteArray): Constants() {
    val data = data

//    val uid by lazy {
//        val sn0 = data.elementAt(0).toInt()
//        val sn1 = data.elementAt(1).toInt()
//        val sn2 = data.elementAt(2).toInt()
//        val cb0 = data.elementAt(3).toInt()
//        val sn3 = data.elementAt(4).toInt()
//        val sn4 = data.elementAt(5).toInt()
//        val sn5 = data.elementAt(6).toInt()
//        val sn6 = data.elementAt(7).toInt()
//        val cb1 = data.elementAt(8).toInt()
//
//        val isValid = ((0x88 xor sn0 xor sn1 xor sn2).equals(cb0)) and
//                      ((sn3 xor sn4 xor sn5 xor sn6).equals(cb1))
//
//        val uid: Long = ((sn6) or (sn5 shl 8) or (sn4 shl 16) or (sn3 shl 24) or
//                        (sn2 shl 32) or (sn1 shl 40) or (sn0 shl 48)).toLong()
//
//        if (isValid)
//            Just(uid)
//        else
//            Invalid
//    }

    val isDetected by lazy {
        val cardTypeIndex = (4 * PAGE_SIZE) + 1
        val cardType = data[cardTypeIndex]

        when (cardType.toInt()) {
            1, 2, 3, 4, 5, 100 -> true
            else -> false
        }
    }

    val isValid: Boolean by lazy {
        val blockBitsIndex = (2 * PAGE_SIZE) + 2
        val blockBitsMsb = data[blockBitsIndex + 0]
        val blockBitsLsb = data[blockBitsIndex + 1]
        blockBitsMsb == 0xF2.toByte() && blockBitsLsb == 0x03.toByte()
    }

    val layout by lazy {
        val layoutTypeIndex = (4 * PAGE_SIZE) + 1
        val layoutType = data[layoutTypeIndex]

        layoutType
    }

    val totalRides by lazy {
        when (layout.toInt()) {
            1, 2, 3 -> "1"
            4       -> "5"
            5,  100 -> "15"
            else    -> "Unknown"
        }
    }

    val remainingRides: Int by lazy {
        val otp = page(index = OTP_OFFSET)
        val rides = getRightZeroes(otp)
        rides
    }

    fun page(index: Int): ByteArray {
        val start = index * PAGE_SIZE - 0
        val end   = start + PAGE_SIZE - 1
        val result = data.sliceArray(indices =  start..end)
        return result
    }

    val mask by lazy {
        val maskIndex = (5 * PAGE_SIZE)
        val mask = data[maskIndex]
        mask
    }

    val tariff by lazy {
        val rideCostIndex = (5 * PAGE_SIZE) + 2
        val rideCost = ByteBuffer.wrap(data, rideCostIndex, 2).short
        rideCost
    }

    fun getRightZeroes(otp: ByteArray): Int {
        return when (mask.toInt()) {
            1, 2 -> {
                val lastTwoBytes = otp.sliceArray(2..3)
                val bits = BitSet.valueOf(lastTwoBytes)
                bits.flip(0, 16)
                val zeroes = bits
                zeroes.cardinality()
            }

            3 -> {
                val lastTwoBytes = otp.sliceArray(0..3)
                val bits = BitSet.valueOf(lastTwoBytes)
                bits.flip(0, 16)
                val zeroes = bits
                zeroes.cardinality()
            }

            else -> { 0 }
        }
    }

    val isBus by lazy { !isMetro }

    val bus by lazy {
        val bus = page(index = 0xD)[1].toInt()

        when (bus) {
            0 -> "None"
            else -> bus.toString()
        }
    }

    val isMetro by lazy {
        val bytes = page(index = 0xD).sliceArray(0..1)
        bytes.toHexString().equals("0321")
    }

    val metroStop by lazy {
        val metroStop = page(index = 0xB)[1].toInt()

        when (metroStop) {
            in 0.rangeTo(metroStopArray.size) -> { metroStopArray[metroStop.dec()] }
            else -> { "None" }
        }
    }

//    val isNew = lazy {
//        when (totalRides) {
//            is Unknown -> Unknown
//            else -> Just(totalRides.equals(remainingRides))
//        }
//    }

    fun firstValidationTime(): Maybe<Int> {
        val firstValidation = page(index = DATE_TIME_OFFSET)

        if (firstValidation.toHexString().equals("00000000")) return None

        val minutes = firstValidation.sliceArray(0..3).getInt(0)
        val unixTimestamp = calendarFromGttEpoch(minutes).timeInMillis / 1000
        return Just(unixTimestamp.toInt())
    }

    fun lastValidationBeforeExpirationTime(): Maybe<Long> {
        val lastValidationBeforeExpiration = page(index = DATE_TIME_BEFORE_EXPIRY_OFFSET)

        if (lastValidationBeforeExpiration.toHexString().equals("00000000")) return None

        val minutes = lastValidationBeforeExpiration.sliceArray(0..3).getInt(0)
        val unixTimestamp = calendarFromGttEpoch(minutes).timeInMillis / 1000
        return Just(unixTimestamp)
    }

    fun validate(): Carnet {
        return  if (firstValidationTime() is None || isValidationExpired())
            validateExpiredOrNew()
        else
            refreshValidation()
    }

    fun isValidationExpired(): Boolean {
        val validationTimeGttEpoch = firstValidationTime()

        val currentTime = System.currentTimeMillis() / 1000

        return when(validationTimeGttEpoch) {
            is None -> true
            is Just<Int> -> {
                val validationTime = calendarFromGttEpoch(validationTimeGttEpoch.value).timeInMillis / 1000
                currentTime <= validationTime + MAX_REMAINING_MINUTES * 60
            }
            else -> true
        }
    }

    private fun validateExpiredOrNew(): Carnet {
        var data = data.clone()

        val updatedPage = currentTimeToMinutesFromGttEpoch()
                .shl(bitCount = 8)
                .toByteArray()

        4.times { index ->
            val byteIndexA = (PAGE_SIZE * DATE_TIME_OFFSET) + index
            val byteIndexC = (PAGE_SIZE * DATE_TIME_BEFORE_EXPIRY_OFFSET) + index
            data.set(index = byteIndexA, value = updatedPage[index])
            data.set(index = byteIndexC, value = updatedPage[index])
        }

        return Carnet(data)
    }

    private fun refreshValidation(): Carnet {
        var data = data.clone()

        val updatedPageC = currentTimeToMinutesFromGttEpoch()
                                    .shl(bitCount = 8)
                                    .toByteArray()

        4.times { index ->
            val byteIndex = (PAGE_SIZE * DATE_TIME_BEFORE_EXPIRY_OFFSET) + index
            data.set(index = byteIndex, value = updatedPageC[index])
        }

        return Carnet(data)
    }

    fun freshValidationTime(): ByteArray {
        return currentTimeToMinutesFromGttEpoch()
                        .shl(bitCount = 8)
                        .toByteArray()
    }

    fun gttTime(): String {
        val minutesSinceGttEpoch = minutesSinceGttEpoch(fromPageIndex = DATE_TIME_OFFSET)
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.ITALY)
        val calendar = calendarFromGttEpoch(minutesSinceGttEpoch)

        return timeFormatter.format(calendar.time)
    }

    fun gttDate(): String {
        val minutesSinceGttEpoch = minutesSinceGttEpoch(fromPageIndex = DATE_TIME_OFFSET)
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.ITALY)
        val calendar = calendarFromGttEpoch(minutesSinceGttEpoch)

        return dateFormatter.format(calendar.time)
    }

    private fun minutesSinceGttEpoch(fromPageIndex: Int): Int {
        return page(index = fromPageIndex).sliceArray(indices = 0..3).getInt(0)
    }

    private fun calendarFromGttEpoch(minutesSinceGttEpoch: Int): Calendar {
        val GTT_EPOCH = "01/01/2005 00:00"
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val calendar = Calendar.getInstance()

        calendar.time = formatter.parse(GTT_EPOCH)
        calendar.add(Calendar.MINUTE, minutesSinceGttEpoch)

        return calendar
    }

    fun currentTimeToMinutesFromGttEpoch(): Int {
        val millisToMinutesFactor = (1000 * 60)
        val gttCalendar = calendarFromGttEpoch(minutesSinceGttEpoch = 0) // gtt epoch
        val unixEpochCalendar = Calendar.getInstance() // now

        val minutesSinceGttEpoch = (unixEpochCalendar.timeInMillis -
                                    gttCalendar.timeInMillis) /
                                    millisToMinutesFactor

        return minutesSinceGttEpoch.toInt()
    }

    override fun toString(): String {
        return data.toHexString()
    }
}