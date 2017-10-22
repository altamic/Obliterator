package it.convergent.obliterator

import it.convergent.obliterator.Maybe.Just
import it.convergent.obliterator.Maybe.None
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by altamic on 08/04/16.
 */

class Carnet(val data: ByteArray) {

    /*   Carnet Structure
     *   ================
     *
     *   Page 0x00: 0x0KXXYYZZ    checksum: 0xZZ = 0x88 ⊕ 0x0K ⊕ 0xXX ⊕ 0xYY
     *   Page 0x01: 0xGGHHIIJJ    UID: CoP = 0x04XXYYGGHHIIJJ in decimal
     *   Page 0x02: 0xLL          checksum: 0xLL = 0xGG ⊕ 0xHH ⊕ 0xII ⊕ 0xJJ
     *                  KK        internal: 0xKK i.e. chip vendor identifier
     *                    F2      Lock byte #1:
     *                                         0xF:         1            1               1              1
     *                                       meaning:   Page7locked, Page6locked,   Page5locked, Page4locked
     *
     *                                         0x2:         0            0               1              0
     *                                       meaning:  OTPlocked, Pages10to15locked, Pages4to9locked, OTPlocked
     *
     *                      03    Lock byte #2:
     *                                         0x0:         0            0               0              0
     *                                       meaning:  Page15locked, Page14locked, Page13locked, Page12locked
     *
     *                                         0x3:         0            0               1              1
     *                                       meaning:  Page11locked, Page10locked,  Page9locked,  Page8locked
     *
     *   Page 0x03: 0xMMNNOOPP    OTP: remaining rides
     *   Page 0x04: 0x01LL0000    Layout -> Carnet rides
     *   Page 0x05: 0xQQ01RRRR    Mask, Tariff
     *   Page 0x06: 0xTTTTTT00    Purchase date in minutes since GTT epoch
     *   Page 0x07: 0x00######
     *   Page 0x08: 0x##00%%%%    Purchase serial number: ########-%%%% where %%%% in decimal
     *   Page 0x09: 0x????????    Unknown: maybe random
     *   Page 0x0A: 0xTTTTTT00    First timestamp in minutes since GTT epoch
     *   Page 0x0B: 0x04F80000    GTT Zone
     *   Page 0x0C: 0xTTTTTT00    Last timestamp in minutes since GTT epoch within 90'
     *   Page 0x0D: 0x00PP00??    GTT bus line
     *   Page 0x0E: 0xF8AE1???
     *   Page 0x0F: 0x??12????    Unknown, it depends perhaps at least from last stamp within 90'
     *
     *   Factory read only pages: from 0x00 to 0x03
     *   Lock bytes (page 0x02) control remaining pages
     *
     */

    val PAGE_SIZE = 4
    val TOTAL_PAGES = 16

    val OTP_OFFSET = 0x03
    val DATE_TIME_OFFSET = 0x0A
    val DATE_TIME_BEFORE_EXPIRY_OFFSET = 0x0C
    val MAX_REMAINING_MINUTES = 90

    val metroStopArray = listOf(
            "Fermi", "Paradiso", "Marche", "Massaua",
            "Pozzo Strada", "Monte Grappa", "Rivoli",
            "Racconigi", "Bernini", "Principi d'Acaja",
            "XVIII Dicembre", "Porta Susa", "Vinzaglio",
            "Re Umberto", "Porta Nuova FS", "Marconi",
            "Nizza", "Dante", "Carducci-Molinette",
            "Spezia", "Lingotto")

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
        when (layout.toInt()) {
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
        val bus = page(index = 0x0D)[1].toInt()

        when (bus) {
            0 -> "None"
            else -> bus.toString()
        }
    }

    val isMetro by lazy {
        val bytes = page(index = 0x0D).sliceArray(0..1)
        bytes.toHexString().equals("0321")
    }

    val metroStop by lazy {
        val metroStop = page(index = 0x0B)[1].toInt()

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

    fun firstObliterationTime(): Maybe<Int> {
        val firstValidation = page(index = DATE_TIME_OFFSET)

        if (firstValidation.toHexString().equals("00000000")) return None

        val minutes = ByteBuffer.wrap(firstValidation, 0, 4)
                                .int
                                .shr(bitCount = 8)
        val unixTimestamp = GttEpoch.calendar(minutesSinceGttEpoch = minutes)
                             .timeInMillis / 1000

        return Just(unixTimestamp.toInt())
    }

    fun lastObliterationBeforeExpirationTime(): Maybe<Int> {
        val lastValidationBeforeExpiration = page(index = DATE_TIME_BEFORE_EXPIRY_OFFSET)

        if (lastValidationBeforeExpiration.toHexString().equals("00000000")) return None

        val minutes = ByteBuffer.wrap(lastValidationBeforeExpiration, 0, 4)
                                .int
                                .shr(bitCount = 8)
        val unixTimestamp = GttEpoch.calendar(minutesSinceGttEpoch = minutes)
                              .timeInMillis / 1000

        return Just(unixTimestamp.toInt())
    }

    fun obliterate(): Carnet {
        return  if (firstObliterationTime() is None || isObliterationExpired())
            obliterateExpiredOrNew()
        else
            refreshObliteration()
    }

    fun isObliterationExpired(): Boolean {
        val validationUnixTimestamp = firstObliterationTime()

        val currentTime = System.currentTimeMillis() / 1000

        return when(validationUnixTimestamp) {
            is None -> true
            is Just<Int> -> {
                currentTime > validationUnixTimestamp.value + MAX_REMAINING_MINUTES * 60
            }
        }
    }

    private fun obliterateExpiredOrNew(): Carnet {
        val data = data.clone()

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

    private fun refreshObliteration(): Carnet {
        val data = data.clone()

        val updatedPageC = currentTimeToMinutesFromGttEpoch()
                                    .shl(bitCount = 8)
                                    .toByteArray()

        4.times { index ->
            val byteIndex = (PAGE_SIZE * DATE_TIME_BEFORE_EXPIRY_OFFSET) + index
            data.set(index = byteIndex, value = updatedPageC[index])
        }

        return Carnet(data)
    }

    fun freshObliterationTime(): ByteArray {
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

    fun minutesSinceGttEpoch(fromPageIndex: Int): Int {
        val page = page(index = fromPageIndex)
        return ByteBuffer.wrap(page, 0, 4)
                         .int
                         .shr(bitCount = 8)
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

    override fun equals(other: Any?): Boolean {
        if (other !is Carnet) return false
        return this.toString().equals(other.toString())
    }

    override fun hashCode(): Int {
        return this.toString().hashCode()
    }
}