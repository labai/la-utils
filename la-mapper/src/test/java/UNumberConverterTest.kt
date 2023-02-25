import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.impl.KotlinUNumberConverterResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * @author Augustus
 *         created on 2022.12.25
 */
class UNumberConverterTest {
    private val unumberConvResolver = KotlinUNumberConverterResolver(LaConverterRegistry.global)

    @Test
    fun test_simple() {
        val convFn = unumberConvResolver.getKConverter(UInt::class, ULong::class) ?: fail("can't get converter")
        assertEquals(42UL, convFn.convert(42u))
    }

    @Test
    fun test_max_num() {
        var convFn = unumberConvResolver.getKConverter(UByte::class, Int::class) ?: fail("can't get converter")
        assertEquals(255, convFn.convert(UByte.MAX_VALUE))

        convFn = unumberConvResolver.getKConverter(UShort::class, Int::class) ?: fail("can't get converter")
        assertEquals(65535, convFn.convert(UShort.MAX_VALUE))

        convFn = unumberConvResolver.getKConverter(UInt::class, Long::class) ?: fail("can't get converter")
        assertEquals(4294967295L, convFn.convert(UInt.MAX_VALUE))

        convFn = unumberConvResolver.getKConverter(ULong::class, BigInteger::class) ?: fail("can't get converter")
        assertEquals(BigInteger("18446744073709551615"), convFn.convert(ULong.MAX_VALUE))
    }

    @Test
    fun test_combinations() {
        val testTypes = setOf(UByte::class, UShort::class, UInt::class, ULong::class)
        val ub5: UByte = 5u
        val us5: UShort = 5u
        val ui5: UInt = 5u
        val ul5: ULong = 5u
        val fives = listOf<Any>(
            5L,
            5,
            5.toShort(),
            5.toByte(),
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
        val converter = unumberConvResolver.getKConverter(from::class, expected::class) ?: fail("cant get converter")
        println("Converting ${from::class} to ${expected::class}")
        val converted = converter.convert(from)
        val msg = "failed conv " + from.javaClass + " to " + converted!!::class + " (expected " + expected::class + ")"
        if (expected.javaClass == BigDecimal::class.java) {
            assertTrue((converted as BigDecimal).compareTo(expected as BigDecimal) == 0, msg)
        } else {
            assertEquals(converted, expected, msg)
        }
    }
}
