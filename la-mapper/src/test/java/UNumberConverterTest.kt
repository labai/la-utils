import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.ConverterUtils
import com.github.labai.utils.mapper.LaMapper.ConverterConfig
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Arrays
import kotlin.test.fail

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class UNumberConverterTest {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val converterUtils = ConverterUtils(LaConverterRegistry.global, ConverterConfig())

    @Test
    fun test_simple() {
        val convFn = converterUtils.getKotlinUNumberConverter(UInt::class, ULong::class) ?: fail("can't get converter")
        assertEquals(42UL, convFn.convert(42u))
    }

    @Test
    fun test_max_num() {
        var convFn = converterUtils.getKotlinUNumberConverter(UByte::class, Int::class) ?: fail("can't get converter")
        assertEquals(255, convFn.convert(UByte.MAX_VALUE))

        convFn = converterUtils.getKotlinUNumberConverter(UShort::class, Int::class) ?: fail("can't get converter")
        assertEquals(65535, convFn.convert(UShort.MAX_VALUE))

        convFn = converterUtils.getKotlinUNumberConverter(UInt::class, Long::class) ?: fail("can't get converter")
        assertEquals(4294967295L, convFn.convert(UInt.MAX_VALUE))

        convFn = converterUtils.getKotlinUNumberConverter(ULong::class, BigInteger::class) ?: fail("can't get converter")
        assertEquals(BigInteger("18446744073709551615"), convFn.convert(ULong.MAX_VALUE))
    }

    @Test
    fun test_combinations() {
        val testTypes = setOf(UByte::class, UShort::class, UInt::class, ULong::class)
        val ub5: UByte = 5u
        val us5: UShort = 5u
        val ui5: UInt = 5u
        val ul5: ULong = 5u
        val fives = Arrays.asList<Any>(
            5L,
            5, 5.toShort(), 5.toByte(),
            BigInteger.valueOf(5),
            BigDecimal(5),
            5.0f,
            5.0,
            ub5,
            us5,
            ui5,
            ul5,
        )

        for (ofr in fives) {
            for (oto in fives) {
                if (ofr::class !in testTypes && oto::class !in testTypes)
                    continue
                testConvNum(ofr, oto)
            }
        }
    }

    private fun testConvNum(from: Any, expected: Any) {
        val converter = converterUtils.getKotlinUNumberConverter(from::class, expected::class) ?: fail("cant get converter")
        println("Converting ${from::class} to ${expected::class}")
        val converted = converter.convert(from)
        val msg = "failed conv " + from.javaClass + " to " + converted!!::class + " (expected " + expected::class + ")"
        if (expected.javaClass == BigDecimal::class.java) {
            assertTrue(msg, (converted as BigDecimal).compareTo(expected as BigDecimal) == 0)
        } else {
            assertEquals(msg, converted, expected)
        }
    }
}
