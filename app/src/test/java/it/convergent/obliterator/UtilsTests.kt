package it.convergent.obliterator

import it.convergent.obliterator.Maybe.Just
import it.convergent.obliterator.Maybe.None
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by altamic on 20/08/16.
 */
class UtilsTests {
    @Test
    @Throws(Exception::class)
    fun maybe() {
        val string = "foo"
        val integer = 42
        val clazz = this@UtilsTests

        val maybeString  = Just(string)
        val maybeInteger = Just(integer)
        val maybeClass   = Just(clazz)
        val none         = None

        assertEquals(string, maybeString.value)
        assertEquals(integer, maybeInteger.value)
        assertEquals(clazz, maybeClass.value)
    }

    @Test
    @Throws(Exception::class)
    fun intTimesReturnValue() {
        val array = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

        array.forEach() { number ->
            assertEquals(number, number.times() { })
        }
    }

    @Test
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun intIsEven() {
        (0..100).step(2).forEach { even ->
            assert(even.isEven())
        }
    }

    @Test
    @Throws(Exception::class)
    fun intIsOdd() {
        (1..100).step(2).forEach { odd ->
            assert(odd.isOdd())
        }
    }

    @Test
    @Throws(Exception::class)
    fun intToByteArray() {

    }

    @Test
    @Throws(Exception::class)
    fun byteArrayGetInt() {

    }

    @Test
    @Throws(Exception::class)
    fun byteArrayGetLong() {

    }

    @Test
    @Throws(Exception::class)
    fun byteArrayToHexString() {

    }

    @Test
    @Throws(Exception::class)
    fun setCarnetToJson() {

    }

    @Test
    @Throws(Exception::class)
    fun stringToJsonCarnetSet() {

    }

    @Test
    @Throws(Exception::class)
    fun rot13() {

    }

    @Test
    @Throws(Exception::class)
    fun gttEpochCurrentTime() {

    }

    @Test
    @Throws(Exception::class)
    fun gttEpochCurrentTimeFromCalendar() {

    }
}