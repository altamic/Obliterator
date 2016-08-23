package it.convergent.obliterator

import it.convergent.obliterator.Maybe.Just
import it.convergent.obliterator.Maybe.None
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by altamic on 20/08/16.
 */

class CarnetTests {
    val newCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001800001050000020102BD5B02840000AE10A6B8004BD38B1AE1F7000000000000000000000000000000000000000000020000"))
    val fourteenRidesRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001C00001050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B0C890004F800005B0C8900003C0004F8AE1074C8128394"))
    val sevenRidesRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F20300FFFF8001050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B2C060004F800005B2C0600003C0004F8AE1078FB126906"))
    val lastRideRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2033FFFFFFE01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B44E20004F800005B44E200003C0004F8AE1070C912FDE6"))
    val emptyCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2037FFFFFFF01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B47DE0004F800005B47DE00003C0004F8AE107B6112086C"))

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

    @Test
    fun page() {
        val carnet = newCarnet
        val pages = listOf(0x046825C1, 0x2A3C3C84, 0xAE48F203, 0x00018000,
                            0x01050000, 0x020102BD, 0x5B028400, 0x00AE10A6,
                            0xB8004BD3, 0x8B1AE1F7, 0x00000000, 0x00000000,
                            0x00000000, 0x00000000, 0x00000000, 0x00020000)

        0x0F.times { pageIndex ->
            4.times { index ->
                assertEquals(pages[pageIndex].toByteArray()[index + 4],
                                carnet.page(index = pageIndex)[index])
            }
        }

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
    fun firstValidationTime() {
        val carnets = listOf(emptyCarnet, fourteenRidesRemainingCarnet,
                        sevenRidesRemainingCarnet, lastRideRemainingCarnet,
                        obliteratedFirstTimeCarnet, obliteratedSecondTimeCarnet)

        carnets.forEach { carnet ->
            assert(carnet.firstValidationTime() is Just<Int>)
            val timestamp = carnet.firstValidationTime() as Just<Int>
            val minutesSinceGttEpoch = carnet.page(index = 0x0A).sliceArray(0..2).getInt(index = 0)
            val obtainedTimestamp = (GttEpoch.calendar(minutesSinceGttEpoch).timeInMillis / 1000).toInt()
            assertEquals(obtainedTimestamp, timestamp.value)
        }

        assertEquals(None, newCarnet.firstValidationTime())
    }

    @Test
    fun lastValidationBeforeExpirationTime(){

    }

    @Test
    fun isValidationExpired() {

    }

    @Test
    fun validate() {

    }

    @Test
    fun freshValidationTime() {

    }

    @Test
    fun gttTime() {

    }

    @Test
    fun gttDate() {

    }

    @Test
    fun currentTimeToMinutesFromGttEpoch() {

    }

    @Test
    fun toStringRepresentation() {

    }
}