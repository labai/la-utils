import com.github.labai.utils.mapper.LaMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperDelegateTest {

    interface IFrom {
        val v1: Int
        val v2: BigDecimal
    }

    interface ITo {
        val v1: BigDecimal
        val v2: BigDecimal
    }

    @Test
    fun test_from_delegate_object() {
        class FromOrig(
            override val v1: Int,
            override val v2: BigDecimal,
        ) : IFrom

        class To(
            override val v1: BigDecimal,
            override val v2: BigDecimal,
        ) : ITo {
            var v3 = 0
        }

        class From(orig: FromOrig) : IFrom by orig {
            val v3 = 1
        }

        val mapper = LaMapper.autoMapper<From, To>()

        val from = From(FromOrig(5, BigDecimal("6.5")))

        val res = mapper.transform(from)

        assertEqBigDecimal("5", res.v1)
        assertEqBigDecimal("6.5", res.v2)
        assertEquals(1, res.v3)
    }

    @Test
    fun test_from_delegate_map() {
        class From(map: MutableMap<String, Any?>) {
            var v1: Int by map
            var v2: BigDecimal by map
            var v3 = 1
        }

        class To(
            val v1: BigDecimal,
            val v2: BigDecimal,
        ) {
            var v3 = 0
        }

        val mapper = LaMapper.autoMapper<From, To>()

        val from = From(mutableMapOf("v1" to 5, "v2" to BigDecimal("6.5")))

        val res = mapper.transform(from)

        assertEqBigDecimal("5", res.v1)
        assertEqBigDecimal("6.5", res.v2)
        assertEquals(1, res.v3)
    }

    @Test
    fun test_from_lazy() {
        class From {
            val v1: Int by lazy { 5 }
            val v2: BigDecimal by lazy { "6.5".toBigDecimal() }
            var v3 = 1
        }

        class To(
            val v1: BigDecimal,
            val v2: BigDecimal,
        ) {
            var v3 = 0
        }

        val mapper = LaMapper.autoMapper<From, To>()

        val from = From()

        val res = mapper.transform(from)

        assertEqBigDecimal("5", res.v1)
        assertEqBigDecimal("6.5", res.v2)
        assertEquals(1, res.v3)
    }

    private fun assertEqBigDecimal(expectedAsStr: String, value: BigDecimal) {
        assertTrue(value.compareTo(BigDecimal(expectedAsStr)) == 0, "Expected '$expectedAsStr', got '$value'")
    }
}
