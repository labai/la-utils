package com.github.labai.deci

import com.github.labai.deci.Deci.DeciContext
import java.math.BigDecimal
import java.math.RoundingMode.DOWN
import java.math.RoundingMode.HALF_UP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @author Augustus
 *         created on 2020.11.18
 */
class DeciTest {

    @Test
    fun test_equals() {
        // eq
        assertTrue(Deci(BigDecimal("12.2")) eq BigDecimal("12.2"))
        assertTrue(Deci(1) eq 1.deci)
        assertTrue(Deci("1.00") eq 1.deci)
        assertTrue(Deci(1) eq 1L.deci)
        assertTrue(1.deci eq 1)
        assertTrue(1.deci eq 1L)
        assertTrue(1.deci eq 1.0)
        assertTrue(1.deci eq 1.toShort())

        // ==
        assertEquals(Deci(1), 1.deci)
        assertEquals(Deci("1.00"), 1.deci)
        assertEquals(Deci(1), 1L.deci)
        assertEquals(Deci("1.00"), Deci("1.000") / 100000 * 100000)
    }

    @Test
    fun test_operators() {
        assertDecEquals("3", 2.deci + 1)
        assertDecEquals("3", 2.deci + 1L)
        assertDecEquals("3", 2.deci + 1.toBigDecimal())

        assertDecEquals("1.4", Deci("1.2") * 2L - 1)

        // unary minus
        assertDecEquals("-1.1", -Deci("1.1"))
    }

    @Test
    fun test_division_simple() {
        assertDecEquals("1", 2.deci / 2)
        assertDecEquals("1.5", 3.deci / 2L)
        assertDecEquals("1.01", Deci("2.02") / 2)
    }

    @Test
    fun test_division_complex() {
        // (1 - (1/365)) * (1 - (2/365)
        val d = (1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) round 11
        assertDecEquals("0.99179583412", d)

        val d2 = (BigDecimal.ONE - BigDecimal.ONE / BigDecimal(365)) * (BigDecimal.ONE - BigDecimal(2) / BigDecimal(365))
        assertDecEquals("1", d2) // WRONG with original BigDecimal!

        val d3 = 1.deci / Deci("1.23e10") * Deci("2.34e-10") * BigDecimal("1e20") round 11
        assertDecEquals("1.90243902439", d3)
    }

    @Test
    fun test_rounding() {
        assertDecEquals("1.11", Deci("1.114").round(2))
        assertDecEquals("1.12", Deci("1.115") round 2)
        assertEquals(BigDecimal("1.11000"), Deci("1.11").round(5).toBigDecimal())
    }

    @Test
    fun test_toBigDecimal() {
        assertEquals(BigDecimal("1.11"), (Deci("1.11") round 2).toBigDecimal())
        assertEquals(BigDecimal("1.1100"), (Deci("1.11") round 4).toBigDecimal())
        assertDecEquals("1.12", Deci("1.115") round 2)
    }

/*
    @Test
    fun test_nulls() {
        val d1: Deci? = null
        val d2: Deci? = Deci("12.2") + d1
        assertNull(d2)

        val d3 = Deci("12.2") / 12 * d1 ?: 0.deci
        assertEquals(0.deci, d3)

        val int1: Int? = null
        assertNull(1.deci + int1)

        val long1: Int? = null
        assertNull(1.deci / long1)

        val d4 = Deci("12.2") / 12 * d1
        assertNull(d4 round 2)
        assertNull(d4 bigd 2)
        assertNull(d4.toBigDecimal())

        val bd1: BigDecimal? = null
        assertTrue(d4 eq bd1)
        assertFalse(d4 eq BigDecimal.ONE)

    }
*/
    @Test
    fun test_valueOf() {
        assertEquals(2.deci, Deci.valueOf(2.toByte()))
        assertEquals(2.deci, Deci.valueOf(2.toShort()))
        assertEquals(2.deci, Deci.valueOf(2.toBigDecimal()))

        assertDecEquals(Deci("2.2"), Deci.valueOf(2.2) round 10)
        assertDecEquals(Deci("2.2"), Deci.valueOf(2.2.toFloat()) round 5)

        // floats are not precise
        assertFalse(Deci("2.2") eq Deci.valueOf(2.2.toFloat()))
    }

    @Test
    fun test_compare() {
        assertTrue(2.deci > 1.toBigDecimal())
        assertTrue(2.deci > 1)
        assertTrue(2.deci >= 2)
        assertTrue(2.deci <= 2L)
        assertTrue(2.deci <= 2.toByte())
        assertTrue(2.deci <= 2.toShort())
        assertTrue(2.deci < 2.2.toDouble())
        assertTrue(2.deci < 2.2.toFloat())
    }

    @Test
    fun test_hashcode() {
        val list = (0..5).map { Deci("$it.${it}000") }
        val map = list.map { it to it * 10 }.toMap()
        // searching in map uses hashcode
        assertEquals(22.deci, map[Deci("2.2")])
        // should be cached
        val d = 22.deci
        assertTrue(d.hashCode() === d.hashCode())
    }

    @Test
    fun test_exceptions() {
        val d1: Deci = 0.deci

        val d2: Deci? = try {
            Deci("12.2") / d1
            throw IllegalStateException("Expected div/0")
        } catch (e: Exception) {
            null
        }
        assertNull(d2)
    }

    @Test
    fun test_deciContext() {
        // should keep first operator DeciContext

        val d1 = Deci(BigDecimal("1.2"), DeciContext(55))
        val d2 = d1 / 7.deci
        assertEquals(55, d2.toBigDecimal().scale())

        val d3 = Deci(BigDecimal("1.192"), DeciContext(1, DOWN, 1))
        assertDecEquals("1.1", d3) // rounded down
    }

    @Test
    fun test_scale() {
        // (ctx4) should keep scale
        //  - if provided < 4 - then use provided scale
        //  - use 4 - if provided scale is bigger
        //      - but keep minimum precision 3 (minimum non zero digits)
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        fun checkScale(expectedScale: Int, num: String) {
            assertEquals(expectedScale, Deci(BigDecimal(num), ctx4).toBigDecimal().scale())
            assertEquals(expectedScale, Deci(BigDecimal("-$num"), ctx4).toBigDecimal().scale()) // check with negative value also
        }

        checkScale(0, "1.1e+5")
        checkScale(4, "123.123456")
        checkScale(2, "1.12")
        checkScale(2, "0.12")
        checkScale(3, "0.120") // trailing zeros are not ignored
        checkScale(3, "0.123")
        checkScale(4, "0.1234")
        checkScale(4, "0.01234")
        checkScale(5, "0.001234")
        checkScale(6, "0.0001234")
        checkScale(7, "0.0000001")
        checkScale(8, "0.00000010")
        checkScale(6, "1.1e-5")
        checkScale(0, "0")
        checkScale(0, "0.0") // zero is zero (ignore trailing zeros for 0)
        checkScale(0, "0.000000")
        checkScale(0, "10")
    }

    @Test
    fun test_divScale() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        fun checkDivScale(expectedScale: Int, num: String, divisor: String) {
            assertEquals(expectedScale, Deci(BigDecimal(num), ctx4).calcDivScale(BigDecimal(divisor)))
            assertEquals(expectedScale, Deci(BigDecimal("-$num"), ctx4).calcDivScale(BigDecimal(divisor))) // check with negative value also
            assertEquals(expectedScale, Deci(BigDecimal(num), ctx4).calcDivScale(BigDecimal("-$divisor")))
            assertEquals(expectedScale, Deci(BigDecimal("-$num"), ctx4).calcDivScale(BigDecimal("-$divisor")))
        }

        checkDivScale(4, "10.1", "12.2")
        checkDivScale(4, "100", "11")
        checkDivScale(5, "10.1", "1000") // 0.01010
        checkDivScale(5, "10.1", "9999") // 0.00101
        checkDivScale(6, "11", "10000") // 0.00110
        checkDivScale(6, "11", "99999") // 0.000110
        checkDivScale(7, "0.011", "100") // 0.0001100
        checkDivScale(7, "0.011", "999") // 0.0000110
        checkDivScale(5, "1", "100") // ?
        checkDivScale(5, "0", "999") //

        checkDivScale(4, "0.011", "0.01") // 1.1000
        checkDivScale(7, "0.0000110", "0.01") // 0.001100 (left original scale)
        checkDivScale(7, "0.0000110", "0.09") // 0.000122 (left original scale)
        checkDivScale(5, "0.00011", "0.01") // 0.01100
        checkDivScale(5, "0.00011", "0.0999") // 0.00110
    }

    @Test
    fun test_round_precedence() {
        // round should be on result after all operators executed, not for last argument
        assertDecEquals(Deci("1.2"), (Deci("1.16") - (Deci("0.02") round 1) round 1)) // when rounded last argument
        assertDecEquals(Deci("1.1"), Deci("1.16") - Deci("0.02") round 1) // when rounded result
    }

    @Test
    fun test_sumOf() {
        val list = listOf(Deci("1.2"), 1.deci)
        assertDecEquals("2.2", list.sumOf { it })
    }

    @Test
    fun test_toString() {
        assertEquals("-12.02", Deci("-12.0200").toString())
        assertEquals("12", Deci("12.0000").toString())
        assertEquals("1200", Deci("1200.0000").toString())
        assertEquals("1200000", Deci("12e5").toString())
    }

    @Test
    fun test_demo1() {
        class Demo1(val quantity: Deci, val price: Deci, val fee: Deci) {
            fun getPercent1(): Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
        }

        val demo = Demo1(Deci("12.2"), Deci("55.97"), Deci("15.5"))

        val res2: BigDecimal = ((demo.price * demo.quantity - demo.fee) * 100 / (demo.price * demo.quantity) round 8).toBigDecimal()

        assertDecEquals("97.73004859", res2)
        assertDecEquals("97.73", demo.getPercent1())
    }

    private fun assertDecEquals(dec1: BigDecimal, dec2: BigDecimal) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: Deci, dec2: BigDecimal) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: BigDecimal, dec2: Deci) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: Deci, dec2: Deci) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: Deci) = assertTrue(Deci(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
    private fun assertDecEquals(dec1: String, dec2: BigDecimal) = assertTrue(BigDecimal(dec1) eq dec2, "Decimals are not equal ($dec1 vs $dec2)")

    private infix fun BigDecimal?.eq(other: BigDecimal?): Boolean {
        if (this == null && other == null) return true
        if (this == null || other == null) return false
        return this.compareTo(other) == 0
    }
}
