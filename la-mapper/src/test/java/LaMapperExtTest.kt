import com.github.labai.deci.Deci
import com.github.labai.deci.deci
import com.github.labai.deci.eq
import com.github.labai.utils.mapper.LaMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperExtTest {

    @Test
    fun test_deci() {
        data class T01From(val v1: Int, val v2: BigDecimal) {
            var a20: Deci? = null
        }

        class T01To(val v1: Deci, val v2: Deci) {
            var a20: Long? = null
        }

        val mapper = LaMapper.autoMapper<T01From, T01To>()

        val from = T01From(5, BigDecimal("6.5")).apply {
            a20 = 7.deci
        }

        val res = mapper.transform(from)

        assertTrue(res.v1 eq 5)
        assertTrue(res.v2 eq Deci("6.5"))
        assertEquals(7L, res.a20)
    }
}
