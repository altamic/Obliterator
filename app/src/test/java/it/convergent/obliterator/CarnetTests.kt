package it.convergent.obliterator

import it.convergent.obliterator.Maybe.Just
import it.convergent.obliterator.Maybe.None
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by altamic on 20/08/16.
 */

class CarnetTests {
    val newCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001800001050000020102BD5B02840000AE10A6B8004BD38B1AE1F7000000000000000000000000000000000000000000020000"))
    val new5Carnet = Carnet(data = hexStringToByteArray("046DDD3C2A753380EC48F20307FFFFE001040000020102BE584EE00000AE10FE20008E3716E55964000000000000000000000000000000000000000000020000"))
    val fourteenRidesRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001C00001050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B0C890004F800005B0C8900003C0004F8AE1074C8128394"))
    val sevenRidesRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F20300FFFF8001050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B2C060004F800005B2C0600003C0004F8AE1078FB126906"))
    val lastRideRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2033FFFFFFE01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B44E20004F800005B44E200003C0004F8AE1070C912FDE6"))
    val emptyCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2037FFFFFFF01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B47DE0004F800005B47DE00003C0004F8AE107B6112086C"))

    val empty15Carnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2037FFFFFFF01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B47DE0004F800005B47DE00003C0004F8AE107B6112086C"))
    val empty5Carnet  = Carnet(data = hexStringToByteArray("046201EF5A7533849848F2037FFFFFFF01040000020102BE58D0400000AE10A6B8003F6795C7D8C65A46D10004F800005A46D10000380004F8AE107B2812D04A"))
    val empty1Carnet  = Carnet(data = hexStringToByteArray("046201EF5A7533849848F2037FFFFFFF01010000020102BE58D0400000AE10A6B8003F6795C7D8C65A46D10004F800005A46D10000380004F8AE107B2812D04A"))

    val obliteratedFirstTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EA00003C0004F8AE10795C129EB3"))
    val obliteratedSecondTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EF00003C0004F8AE10795C1225F9"))

    val _15_rides_minus_00 = 0b00000000000000011000000000000000 // 0x00018000
    val _15_rides_minus_01 = 0b00000000000000011100000000000000 // 0x0001C000
    val _15_rides_minus_02 = 0b00000000000000111110000000000000 // 0x0003E000
    val _15_rides_minus_03 = 0b00000000000001111111000000000000 // 0x0007F000
    val _15_rides_minus_04 = 0b00000000000011111111100000000000 // 0x000FF800
    val _15_rides_minus_05 = 0b00000000000111111111110000000000 // 0x001FFC00
    val _15_rides_minus_06 = 0b00000000001111111111111000000000 // 0x003FFE00
    val _15_rides_minus_07 = 0b00000000011111111111111100000000 // 0x007FFF00
    val _15_rides_minus_08 = 0b00000000111111111111111110000000 // 0x00FFFF80
    val _15_rides_minus_09 = 0b00000001111111111111111111000000 // 0x01FFFFC0
    val _15_rides_minus_10 = 0b00000011111111111111111111100000 // 0x03FFFFE0
    val _15_rides_minus_11 = 0b00000111111111111111111111110000 // 0x07FFFFF0
    val _15_rides_minus_12 = 0b00001111111111111111111111111000 // 0x0FFFFFF8
    val _15_rides_minus_13 = 0b00011111111111111111111111111100 // 0x1FFFFFFC
    val _15_rides_minus_14 = 0b00111111111111111111111111111110 // 0x3FFFFFFE
    val _15_rides_minus_15 = 0b01111111111111111111111111111111 // 0x7FFFFFFF

    fun currentlyValidCarnet(): Carnet {
        val currentGttMinutes = GttEpoch.currentTime()
                                        .shl(bitCount = 8)
                                        .toByteArray()
                                        .toHexString()
                                        .removePrefix(prefix = "00")

        return Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001C00001050000020102BD5B02840000AE10A6B8004BD38B1AE1F7${currentGttMinutes}0004F80000${currentGttMinutes}00003C0004F8AE1074C8128394"))
    }

    fun currentlyValidCarnetMinus30Minutes(): Carnet {
        val millisToMinutesFactor = (1000 * 60)
        val gttCalendar = GttEpoch.calendar(minutesSinceGttEpoch = 0) // gtt epoch
        val unixEpochCalendar = Calendar.getInstance()
        unixEpochCalendar.add(Calendar.MINUTE, -30)

        val minutesSinceGttEpochMinus30Minutes = ((unixEpochCalendar.timeInMillis -
                gttCalendar.timeInMillis) / millisToMinutesFactor).toInt()

        val gttMinutes = minutesSinceGttEpochMinus30Minutes
                            .toByteArray()
                            .toHexString()
                            .removePrefix(prefix = "00")

        return Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001C00001050000020102BD5B02840000AE10A6B8004BD38B1AE1F7${gttMinutes}0004F80000${gttMinutes}00003C0004F8AE1074C8128394"))
    }

    @Test
    fun uid() {
        assertEquals("046825C12A3C3C84AE", newCarnet.uid.toHexString())
        assertEquals("046DDD3C2A753380EC", new5Carnet.uid.toHexString())
        assertEquals("046825C12A3C3C84AE", fourteenRidesRemainingCarnet.uid.toHexString())
        assertEquals("046825C12A3C3C84AE", sevenRidesRemainingCarnet.uid.toHexString())
        assertEquals("046825C12A3C3C84AE", lastRideRemainingCarnet.uid.toHexString())
        assertEquals("046825C12A3C3C84AE", emptyCarnet.uid.toHexString())
        assertEquals("046825C12A3C3C84AE", empty15Carnet.uid.toHexString())
        assertEquals("046201EF5A75338498", empty5Carnet.uid.toHexString())
        assertEquals("046201EF5A75338498", empty1Carnet.uid.toHexString())
    }

    @Test
    fun pageSize() {
        val PAGE_SIZE = 4
        assertEquals(PAGE_SIZE, newCarnet.PAGE_SIZE)
    }

    @Test
    fun totalPages() {
        val TOTAL_PAGES = 16
        assertEquals(TOTAL_PAGES, newCarnet.TOTAL_PAGES)
    }

    @Test
    fun isDetected() {
        val cardTypesTrue = listOf(1, 2, 3, 4, 5, 100)
        val cardTypesFalse = listOf(7, 9, 11, 101)
        val cardTypeIndex = (4 * 4) + 1

        cardTypesTrue.forEach { cardType ->
            val data = hexStringToByteArray("046825C12A3C3C84AE48F20300018000FF050000020102BD5B02840000AE10A6B8004BD38B1AE1F7000000000000000000000000000000000000000000020000")
            data[cardTypeIndex] = cardType.toByte()
            val carnet = Carnet(data = data)
            assert(carnet.isDetected)
        }

        cardTypesFalse.forEach { cardType ->
            val data = hexStringToByteArray("046825C12A3C3C84AE48F20300018000FF050000020102BD5B02840000AE10A6B8004BD38B1AE1F7000000000000000000000000000000000000000000020000")
            data[cardTypeIndex] = cardType.toByte()
            val carnet = Carnet(data = data)
            assert(!carnet.isDetected)
        }
    }

    @Test
    fun isValid() {
        assert(newCarnet.isValid)

        val invalid = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48FA030001800001050000020102BD5B02840000AE10A6B8004BD38B1AE1F7000000000000000000000000000000000000000000020000"))
        assert(!invalid.isValid)
    }

    @Test
    fun totalRides() {
        assertEquals("15", empty15Carnet.totalRides)
        assertEquals( "5", empty5Carnet.totalRides)
        assertEquals( "1", empty1Carnet.totalRides)

        assertEquals("15", sevenRidesRemainingCarnet.totalRides)
        assertEquals( "5", new5Carnet.totalRides)

        val unknown  = Carnet(data = hexStringToByteArray("046201EF5A7533849848F2037FFFFFFF01AE0000020102BE58D0400000AE10A6B8003F6795C7D8C65A46D10004F800005A46D10000380004F8AE107B2812D04A"))
        assertEquals( "Unknown", unknown.totalRides)
    }

    @Test
    fun remainingBusRides() {
        assertEquals(15, newCarnet.remainingBusRides)
        assertEquals( 5, new5Carnet.remainingBusRides)
        assertEquals(14, fourteenRidesRemainingCarnet.remainingBusRides)
        assertEquals( 7, sevenRidesRemainingCarnet.remainingBusRides)
        assertEquals( 1, lastRideRemainingCarnet.remainingBusRides)
        assertEquals( 0, emptyCarnet.remainingBusRides)
        assertEquals( 0, empty15Carnet.remainingBusRides)
        assertEquals( 0, empty1Carnet.remainingBusRides)
    }

    @Test
    fun remainingMetroRides() {
        assertEquals(15, newCarnet.remainingMetroRides)
        assertEquals( 5, new5Carnet.remainingMetroRides)
        assertEquals(15, fourteenRidesRemainingCarnet.remainingMetroRides) // 15 metro when 14 bus?!
        assertEquals( 8, sevenRidesRemainingCarnet.remainingMetroRides)
        assertEquals( 2, lastRideRemainingCarnet.remainingMetroRides)
        assertEquals( 1, emptyCarnet.remainingMetroRides)
        assertEquals( 1, empty15Carnet.remainingMetroRides)
        assertEquals( 1, empty1Carnet.remainingMetroRides)
    }

    @Test
    fun page() {
        val carnet = fourteenRidesRemainingCarnet
        val pages = listOf(0x046825C1, 0x2A3C3C84, 0xAE48F203, 0x0001C000,
                           0x01050000, 0x020102BD, 0x5B028400, 0x00AE10A6,
                           0xB8004BD3, 0x8B1AE1F7, 0x5B0C8900, 0x04F80000,
                           0x5B0C8900, 0x003C0004, 0xF8AE1074, 0xC8128394)

        0x0F.times { pageIndex ->
            4.times { index ->
                assertEquals(pages[pageIndex].toByteArray()[index + 4],
                                carnet.page(index = pageIndex)[index])
            }
        }

    }


    @Test
    fun mask() {

    }

    @Test
    fun tariff() {

    }

    @Test
    fun rightZeroes() {
        val carnet = newCarnet

        val map = mapOf(0x0F to _15_rides_minus_00,
                        0x0E to _15_rides_minus_01,
                        0x0D to _15_rides_minus_02,
                        0x0C to _15_rides_minus_03,
                        0x0B to _15_rides_minus_04,
                        0x0A to _15_rides_minus_05,
                        0x09 to _15_rides_minus_06,
                        0x08 to _15_rides_minus_07,
                        0x07 to _15_rides_minus_08,
                        0x06 to _15_rides_minus_09,
                        0x05 to _15_rides_minus_10,
                        0x04 to _15_rides_minus_11,
                        0x03 to _15_rides_minus_12,
                        0x02 to _15_rides_minus_13,
                        0x01 to _15_rides_minus_14,
                        0x00 to _15_rides_minus_15)

        map.forEach { item ->
            assertEquals(item.key, carnet.getRightZeroes(otp = item.value.toByteArray()))
        }
    }

    @Test
    fun firstObliterationTime() {
        val carnets = listOf(emptyCarnet, fourteenRidesRemainingCarnet,
                        sevenRidesRemainingCarnet, lastRideRemainingCarnet,
                        obliteratedFirstTimeCarnet, obliteratedSecondTimeCarnet)

        carnets.forEach { carnet ->
            assert(carnet.firstObliterationTime() is Just<Int>)
            val timestamp = carnet.firstObliterationTime() as Just<Int>
            val minutesSinceGttEpoch = ByteBuffer.wrap(carnet.page(index = 0x0A), 0, 4)
                                                 .int
                                                 .shr(bitCount = 8)
            val obtainedTimestamp = (GttEpoch.calendar(minutesSinceGttEpoch).timeInMillis / 1000).toInt()
            assertEquals(obtainedTimestamp, timestamp.value)
        }

        assertEquals(None, newCarnet.firstObliterationTime())
    }

    @Test
    fun lastObliterationBeforeExpirationTime(){
        val obliteratedOneTime = obliteratedFirstTimeCarnet
        val minutesSinceGttEpoch = ByteBuffer.wrap(obliteratedOneTime.page(index = 0x0A), 0, 4)
                                             .int
                                             .shr(bitCount = 8)
        val obtainedTimestamp = (GttEpoch.calendar(minutesSinceGttEpoch).timeInMillis / 1000).toInt()

        assert(obliteratedOneTime.firstObliterationTime() is Just<Int>)
        val timestamp = obliteratedOneTime.firstObliterationTime() as Just<Int>
        assertEquals(obtainedTimestamp, timestamp.value)

        val obliteratedSecondTime = obliteratedSecondTimeCarnet
        val minutesSinceGttEpoch2 = ByteBuffer.wrap(obliteratedSecondTime.page(index = 0x0C), 0, 4)
                                              .int
                                              .shr(bitCount = 8)
        val obtainedTimestamp2 = (GttEpoch.calendar(minutesSinceGttEpoch2).timeInMillis / 1000).toInt()

        assert(obliteratedOneTime.lastObliterationBeforeExpirationTime() is Just<Int>)
        val timestamp2 = obliteratedSecondTimeCarnet.lastObliterationBeforeExpirationTime() as Just<Int>
        assertEquals(obtainedTimestamp2, timestamp2.value)

        assertEquals(None, newCarnet.firstObliterationTime())
    }

    @Test
    fun isValidationExpired() {
        assert(newCarnet.isObliterationExpired())

        assert(emptyCarnet.isObliterationExpired())

        val currentlyValid = currentlyValidCarnet()
        assert(!currentlyValid.isObliterationExpired())
    }

    @Test
    fun obliterate() {
        // never validated
        assertEquals(None, newCarnet.firstObliterationTime())
        val obliterated = newCarnet.obliterate()
        val obliterationDateTime = obliterated.firstObliterationTime()

        val currentMinute = (Calendar.getInstance().timeInMillis / 1000) / 60 * 60

        assert(obliterationDateTime is Just<Int>)
        val timestamp = obliterationDateTime as Just<Int>
        assertEquals(currentMinute, timestamp.value.toLong())

        // expired validation
        assert(emptyCarnet.isObliterationExpired())
        val obliterated2 = emptyCarnet.obliterate()
        val obliterationDateTime2 = obliterated2.firstObliterationTime()

        val currentMinute2 = (Calendar.getInstance().timeInMillis / 1000) / 60 * 60

        assert(obliterationDateTime2 is Just<Int>)
        val timestamp2 = obliterationDateTime2 as Just<Int>
        assertEquals(currentMinute2, timestamp2.value.toLong())

        // refresh validation with a still valid carnet
        val currentlyValid = currentlyValidCarnetMinus30Minutes()
        val originalValidationPage = currentlyValid.page(index = 0x0A)

        val refreshedObliteration = currentlyValid.obliterate()

        // page 0x0A should be invariant
        assertEquals(originalValidationPage.toHexString(),
                        refreshedObliteration.page(index = 0x0A).toHexString())

        // page 0x0C should be updated
        val currentMinute3 = (Calendar.getInstance().timeInMillis / 1000) / 60 * 60
        val refreshedObliterationDateTime = refreshedObliteration.lastObliterationBeforeExpirationTime()
        assert(refreshedObliterationDateTime is Just<Int>)
        val timestamp3 = refreshedObliterationDateTime as Just<Int>

        assertEquals(currentMinute3, timestamp3.value.toLong())
    }

    @Test
    fun freshObliterationTime() {

    }

    @Test
    fun minutesFromGttEpoch() {
        assertEquals(0, newCarnet.minutesSinceGttEpoch(0x0A))
        assertEquals(0, newCarnet.minutesSinceGttEpoch(0x0C))

        assertEquals(0x5A02EA, obliteratedFirstTimeCarnet.minutesSinceGttEpoch(0x0A))
        assertEquals(0x5A02EA, obliteratedFirstTimeCarnet.minutesSinceGttEpoch(0x0C))

        assertEquals(0x5A02EA, obliteratedSecondTimeCarnet.minutesSinceGttEpoch(0x0A))
        assertEquals(0x5A02EF, obliteratedSecondTimeCarnet.minutesSinceGttEpoch(0x0C))
    }


    @Test
    fun gttTime() {
        val pageA = lastRideRemainingCarnet.page(index = 0x0A)
        assertEquals("5B44E200", pageA.toHexString()) // 2016-05-16T19:10:00 (Monday) +1h

        assertEquals("19:10", lastRideRemainingCarnet.gttTime())

    }

    @Test
    fun gttDate() {
        val pageA = lastRideRemainingCarnet.page(index = 0x0A)
        assertEquals("5B44E200", pageA.toHexString()) // 2016-05-16T19:10:00 (Monday) +1h

        assertEquals("16 mag 2016", lastRideRemainingCarnet.gttDate())
    }

    @Test
    fun toStringRepresentation() {
        val hex = "046825C12A3C3C84AE48F2033FFFFFFE01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B44E20004F800005B44E200003C0004F8AE1070C912FDE6"
        val carnet = Carnet(data = hexStringToByteArray(hex))

        assertEquals(hex, carnet.toString())
    }
}