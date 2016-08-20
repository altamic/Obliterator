package it.convergent.obliterator

import org.junit.Assert
import org.junit.Test

/**
 * Created by altamic on 20/08/16.
 */

class CarnetTests {
    fun setup() {
        val newCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001800001050000020102BD5B02840000AE10A6B8004BD38B1AE1F7000000000000000000000000000000000000000000020000"))
        val fourteenRidesRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2030001C00001050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B0C890004F800005B0C8900003C0004F8AE1074C8128394"))
        val sevenRidesRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F20300FFFF8001050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B2C060004F800005B2C0600003C0004F8AE1078FB126906"))
        val lastRideRemainingCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2033FFFFFFE01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B44E20004F800005B44E200003C0004F8AE1070C912FDE6"))
        val emptyCarnet = Carnet(data = hexStringToByteArray("046825C12A3C3C84AE48F2037FFFFFFF01050000020102BD5B02840000AE10A6B8004BD38B1AE1F75B47DE0004F800005B47DE00003C0004F8AE107B6112086C"))

        val obliteratedFirstTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EA00003C0004F8AE10795C129EB3"))
        val obliteratedSecondTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EF00003C0004F8AE10795C1225F9"))
    }

    @Test
    @Throws(Exception::class)
    fun page() {
        Assert.assertEquals(4, (2 + 2).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun rightZeroes() {

    }

    @Test
    @Throws(Exception::class)
    fun firstValidationTime() {

    }

    fun lastValidationBeforeExpirationTime(){

    }

    fun isValidationExpired() {

    }

    fun validate() {

    }

    fun freshValidationTime() {

    }

    fun gttTime() {

    }

    fun gttDate() {

    }

    fun currentTimeToMinutesFromGttEpoch() {

    }

    fun toStringRepresentation() {

    }
}