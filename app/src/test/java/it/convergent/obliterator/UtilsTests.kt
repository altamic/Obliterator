package it.convergent.obliterator

import it.convergent.obliterator.Maybe.Just
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by altamic on 20/08/16.
 */
class UtilsTests {
    @Test
    fun maybe() {
        val string = "foo"
        val integer = 42
        val clazz = this@UtilsTests

        val maybeString  = Just(string)
        val maybeInteger = Just(integer)
        val maybeClass   = Just(clazz)

        assertEquals(string, maybeString.value)
        assertEquals(integer, maybeInteger.value)
        assertEquals(clazz, maybeClass.value)
    }

    @Test
    fun intTimesReturnValue() {
        val array = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

        array.forEach() { number ->
            assertEquals(number, number.times() { })
        }
    }

    @Test
    fun intTimesIterations() {
        val map = mapOf(0 to "",
                        1 to "0",
                        2 to "01",
                        3 to "012",
                        4 to "0123",
                        5 to "01234",
                        6 to "012345",
                        7 to "0123456",
                        8 to "01234567",
                        9 to "012345678")

        map.forEach { number ->
            val value = StringBuilder().append("")

            number.key.times { value.append(it) }

            assertEquals(number.value, value.toString())
        }
    }

    @Test
    fun intIsEven() {
        (0..100).step(2).forEach { even ->
            assert(even.isEven())
        }
    }

    @Test
    fun intIsOdd() {
        (1..100).step(2).forEach { odd ->
            assert(odd.isOdd())
        }
    }

    @Test
    fun intToByteArray() {
        val number = 0x654321
        val bytes = arrayOf<Byte>(0x00, 0x65, 0x43, 0x21)

        for (i in 0..bytes.size - 1) {
            assertEquals(bytes[i], number.toByteArray()[i])
        }
    }

    @Test
    fun longToByteArray() {
        val number = 0xB8004BD3
        val bytes = arrayOf<Byte>(0x00, 0x00, 0x00, 0x00,
                            0xB8.toByte(), 0x00, 0x4B, 0xD3.toByte())

        for (i in 0..bytes.size - 1) {
            assertEquals(bytes[i], number.toByteArray()[i])
        }
    }

    @Test
    fun byteArrayGetInt() {
        val bytes = arrayOf<Byte>(0x12, 0x34, 0x56, 0x78)

        assertEquals(0x3412, bytes.toByteArray().getInt(index = 0))
        assertEquals(0x5634, bytes.toByteArray().getInt(index = 1))
        assertEquals(0x7856, bytes.toByteArray().getInt(index = 2))

    }

    @Test
    fun byteArrayGetLong() {
        val bytes = arrayOf(0x01, 0x23, 0x45, 0x67,
                            0x89.toByte(), 0xAB.toByte(),
                            0xCD.toByte(), 0xEF.toByte())

        assertEquals(0x67452301, bytes.toByteArray().getLong(index = 0))
        assertEquals(0x89674523, bytes.toByteArray().getLong(index = 1))
        assertEquals(0xAB896745, bytes.toByteArray().getLong(index = 2))
        assertEquals(0xCDAB8967, bytes.toByteArray().getLong(index = 3))
        assertEquals(0xEFCDAB89, bytes.toByteArray().getLong(index = 4))

    }

    @Test
    fun byteArrayToHexString() {
        val bytes = arrayOf<Byte>(0x12, 0x34, 0x56, 0x78).toByteArray()

        assertEquals("12345678", bytes.toHexString())

        val bytes2 = arrayOf(0xFF.toByte(), 0xEE.toByte(),
                            0xDD.toByte()).toByteArray()

        assertEquals("FFEEDD", bytes2.toHexString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun hexStringToByteArray() {
        val bytes = arrayOf<Byte>(0x12, 0x34, 0x56, 0x78).toByteArray()

        for (i in 0..bytes.size - 1)
            assertEquals(bytes[i], hexStringToByteArray("12345678")[i])

        val illegal = "ABCDEFGH"
        hexStringToByteArray(illegal)
    }

    @Test
    fun setCarnetToJson() {
        val obliteratedFirstTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EA00003C0004F8AE10795C129EB3"))
        val obliteratedSecondTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EF00003C0004F8AE10795C1225F9"))

        val carnets = setOf(obliteratedFirstTimeCarnet, obliteratedSecondTimeCarnet)

        val json = "[\"048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EA00003C0004F8AE10795C129EB3\", \"048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EF00003C0004F8AE10795C1225F9\"]"

        assertEquals(json, carnets.toJson())
    }

    @Test
    fun stringToCarnetSet() {
        val obliteratedFirstTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EA00003C0004F8AE10795C129EB3"))
        val obliteratedSecondTimeCarnet = Carnet(data = hexStringToByteArray("048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EF00003C0004F8AE10795C1225F9"))

        val carnets = setOf(obliteratedFirstTimeCarnet, obliteratedSecondTimeCarnet)

        val json = "[\"048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EA00003C0004F8AE10795C129EB3\", " +
                   "\"048A828462753380A448F2031FFFFFFC01050000020102BD59C2200000AE10A6B80044F3705BE1355A02EA0004F800005A02EF00003C0004F8AE10795C1225F9\"]"

        assertEquals(carnets, json.toCarnetSet())
    }

    @Test
    fun rot13() {
        val hello = "hello"

        assertEquals(hello, rot13(rot13(hello)))
    }

    @Test
    fun gttEpochCurrentTime() {
        val GTT_EPOCH = "01/01/2005 00:00"
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val gttCalendar = Calendar.getInstance()
        gttCalendar.time = formatter.parse(GTT_EPOCH)

        val unixEpochCalendar = Calendar.getInstance() // now
        val millisToMinutesFactor = (1000 * 60)

        val minutesSinceGttEpoch = ((unixEpochCalendar.timeInMillis -
                gttCalendar.timeInMillis) / millisToMinutesFactor).toInt()

        val minutes = minutesSinceGttEpoch.toByteArray()
                        .getInt(index = 0)

        assertEquals(minutes, GttEpoch.currentTime())

    }

    @Test
    fun gttEpochCurrentTimeFromCalendar() {
        val GTT_EPOCH = "01/01/2005 00:00"
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val gttCalendar = Calendar.getInstance()
        gttCalendar.time = formatter.parse(GTT_EPOCH)

        val millisToMinutesFactor = (1000 * 60)
        val calendar = Calendar.getInstance()
        val date = "22/08/2016 21:48"
        val formatter1 = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        calendar.time = formatter1.parse(date)

        val minutesSinceGttEpoch = ((calendar.timeInMillis -
                gttCalendar.timeInMillis) / millisToMinutesFactor).toInt()

        assertEquals(0x5D6CC0, minutesSinceGttEpoch)
    }
}