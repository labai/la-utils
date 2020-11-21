package com.github.labai.deci

import com.github.labai.deci.Deci.Companion
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

/*
 * @author Augustus
 *   com.github.labai
 *   created on 2020.11.18
 *
 * wrapped BigDecimal with features:
 *  - use HALF_UP rounding
 *  - division result with high scale (30)
 *  - math operators with BigDecimal, Int, Long
 *  - null support
 *  - equal ('==') ignores scale (uses compareTo)
 *
 * E.g.
 *  val d1: Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
 *  val d2: BigDecimal = (1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) bigd 11
 *
 * Additional infix functions
 *   round - round number by provided number of decimal, return Deci
 *   bigd - round and convert to BigDecimal
 *   eq - comparison between numbers (various types, including null)
 *
 */
class Deci(decimal: BigDecimal) : Number(), Comparable<Deci> {

    constructor(str: String) : this(BigDecimal(str))
    constructor(int: Int) : this(BigDecimal(int))
    constructor(long: Long) : this(BigDecimal(long))

    private val decimal: BigDecimal = when {
        decimal.scale() < 0 -> decimal.setScale(0, HALF_UP)
        decimal.scale() > maxScale -> decimal.setScale(maxScale, HALF_UP)
        else -> decimal
    }
    private var _hashCode: Int? = null

    operator fun unaryMinus(): Deci = Deci(decimal.negate())

    operator fun plus(other: BigDecimal): Deci = Deci(decimal.add(other))
    operator fun minus(other: BigDecimal): Deci = Deci(decimal.subtract(other))
    operator fun times(other: BigDecimal): Deci = Deci(decimal.multiply(other))
    operator fun div(other: BigDecimal): Deci = Deci(decimal.divide(other, maxScale, HALF_UP))
    operator fun rem(other: BigDecimal): Deci = Deci(decimal.remainder(other))

    operator fun plus(other: Deci): Deci = Deci(decimal.add(other.decimal))
    operator fun minus(other: Deci): Deci = Deci(decimal.subtract(other.decimal))
    operator fun times(other: Deci): Deci = Deci(decimal.multiply(other.decimal))
    operator fun div(other: Deci): Deci = Deci(decimal.divide(other.decimal, maxScale, HALF_UP))
    operator fun rem(other: Deci): Deci = Deci(decimal.remainder(other.decimal))

    inline operator fun plus(other: Int): Deci = plus(BigDecimal(other))
    inline operator fun minus(other: Int): Deci = minus(BigDecimal(other))
    inline operator fun times(other: Int): Deci = times(BigDecimal(other))
    inline operator fun div(other: Int): Deci = div(BigDecimal(other))
    inline operator fun rem(other: Int): Deci = rem(BigDecimal(other))

    inline operator fun plus(other: Long): Deci = plus(BigDecimal(other))
    inline operator fun minus(other: Long): Deci = minus(BigDecimal(other))
    inline operator fun times(other: Long): Deci = times(BigDecimal(other))
    inline operator fun div(other: Long): Deci = div(BigDecimal(other))
    inline operator fun rem(other: Long): Deci = rem(BigDecimal(other))

    override fun toByte(): Byte = decimal.toByte()
    override fun toChar(): Char = decimal.toChar()
    override fun toDouble(): Double = decimal.toDouble()
    override fun toFloat(): Float = decimal.toFloat()
    override fun toInt(): Int = decimal.toInt()
    override fun toLong(): Long = decimal.toLong()
    override fun toShort(): Short = decimal.toShort()

    fun toBigDecimal(): BigDecimal = decimal

    /** round to n decimals and convert to BigDecimal */
    infix fun bigd(scale: Int): BigDecimal = this.decimal.setScale(scale, HALF_UP)

    /** rounds to n decimals. Unlike BigDecimal.round(), here parameter 'scale' means scale, not precision */
    infix fun round(scale: Int): Deci = Deci(this.decimal.setScale(scale, HALF_UP))

    override fun compareTo(other: Deci): Int = decimal.compareTo(other.decimal)

    override fun toString(): String = decimal.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Deci
        return decimal.compareTo(other.decimal) == 0
    }

    override fun hashCode(): Int {
        if (_hashCode == null)
            _hashCode = decimal.stripTrailingZeros().hashCode()
        return _hashCode!!
    }

    companion object {
        private const val maxScale = 30

        private val d0 = Deci(0)
        private val d1 = Deci(1)
        private val d100 = Deci(100)

        fun valueOf(int: Int): Deci {
            return when(int) {
                0 -> d0
                1 -> d1
                100 -> d100
                else -> Deci(int)
            }
        }

        fun valueOf(long: Long): Deci {
            return when(long) {
                0L -> d0
                1L -> d1
                100L -> d100
                else -> Deci(long)
            }
        }
    }
}

//
// Deci nullable
//

operator fun Deci?.unaryMinus(): Deci? = this?.unaryMinus()

operator fun Deci?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.plus(other)
operator fun Deci?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.minus(other)
operator fun Deci?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.times(other)
operator fun Deci?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.div(other)
operator fun Deci?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.rem(other)

operator fun Deci?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.plus(other)
operator fun Deci?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.minus(other)
operator fun Deci?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.times(other)
operator fun Deci?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.div(other)
operator fun Deci?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.rem(other)

operator fun Deci?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.plus(BigDecimal(other))
operator fun Deci?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.minus(BigDecimal(other))
operator fun Deci?.times(other: Int?): Deci? = if (this == null || other == null) null else this.times(BigDecimal(other))
operator fun Deci?.div(other: Int?): Deci? = if (this == null || other == null) null else this.div(BigDecimal(other))
operator fun Deci?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.rem(BigDecimal(other))

operator fun Deci?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.plus(BigDecimal(other))
operator fun Deci?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.minus(BigDecimal(other))
operator fun Deci?.times(other: Long?): Deci? = if (this == null || other == null) null else this.times(BigDecimal(other))
operator fun Deci?.div(other: Long?): Deci? = if (this == null || other == null) null else this.div(BigDecimal(other))
operator fun Deci?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.rem(BigDecimal(other))

infix fun Deci?.bigd(scale: Int): BigDecimal? = this?.bigd(scale)
infix fun Deci?.round(scale: Int): Deci? = this?.round(scale)
infix fun Deci?.eq(other: Deci?): Boolean = if (this == null || other == null) this == other else this.compareTo(other) == 0
infix fun Deci?.eq(other: BigDecimal?): Boolean = if (this == null || other == null) (this == null && other == null) else this.toBigDecimal().compareTo(other) == 0
infix fun Deci?.eq(other: Number?): Boolean = if (this == null || other == null) (this == null && other == null) else this.compareTo(other) == 0

fun Deci?.toBigDecimal(): BigDecimal? = this?.toBigDecimal()


//
// BigDecimal extensions
//

val BigDecimal.deci: Deci
    inline get() = Deci(this)

infix fun BigDecimal.eq(other: Deci) = this.compareTo(other.toBigDecimal()) == 0

//
// Int, Long extensions
//

val Int.deci: Deci
    inline get() = Deci.valueOf(this)

val Long.deci: Deci
    inline get() = Deci.valueOf(this)

//
// additional Deci methods
//
fun Companion.valueOf(number: Number): Deci {
    return when(number) {
        is Deci -> number
        is BigDecimal -> Deci(number)
        is Int -> valueOf(number as Int)
        is Long -> valueOf(number as Long)
        is Double -> Deci(BigDecimal.valueOf(number))
        is Float -> Deci(BigDecimal.valueOf(number.toDouble()))
        is Short -> valueOf(number.toInt() as Int)
        is Byte -> valueOf(number.toInt() as Int)
        else -> Deci(BigDecimal(number.toString()))
    }
}

operator fun Deci.compareTo(other: Number): Int {
    return when(other) {
        is Deci -> compareTo(other as Deci)
        is BigDecimal -> compareTo(other.deci)
        is Int -> compareTo(other.deci)
        is Long -> compareTo(BigDecimal(other).deci)
        is Double -> compareTo(BigDecimal(other).deci)
        is Float -> compareTo(BigDecimal(other.toDouble()).deci)
        is Short -> compareTo(BigDecimal(other.toInt()).deci)
        is Byte -> compareTo(BigDecimal(other.toInt()).deci)
        else -> this.compareTo(Deci(other.toString()))
    }
}
