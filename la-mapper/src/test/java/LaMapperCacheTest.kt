import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapperJ
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Augustus
 *         created on 2022.12.26
 */
class LaMapperCacheTest {

    class DtoFrom(val arg1: Int)
    class DtoTo(val arg1: String) {
        var fld1: String? = null
        override fun toString(): String = "DtoTo(arg1='$arg1', fld1=$fld1)"
    }

    @Test
    internal fun test_few_mappers_caching() {
        val initialSize = LaMapper.global.cache.getMapSize()
        val from = DtoFrom(5)
        val to1 = LaMapper.copyFrom(from) {
            DtoTo::arg1 from { "A=${it.arg1}" }
        }
        val to2 = LaMapper.copyFrom(from) {
            DtoTo::arg1 from { "B=${it.arg1}" }
        }

        assertEquals("A=5", to1.arg1)
        assertEquals("B=5", to2.arg1)

        assertEquals(2, LaMapper.global.cache.getMapSize() - initialSize)

        for (i in 1..2) {
            LaMapper.copyFrom(from) {
                DtoTo::arg1 from { "C=${it.arg1}" }
            }
        }
        assertEquals(3, LaMapper.global.cache.getMapSize() - initialSize)

        for (i in 1..2) {
            LaMapper.copyFrom<DtoFrom, DtoTo>(from)
        }
        assertEquals(4, LaMapper.global.cache.getMapSize() - initialSize)
    }

    @Test
    internal fun test_mappers_caching_closure() {
        val initialSize = LaMapper.global.cache.getMapSize()
        val from = DtoFrom(5)

        for (i in 1..2) {
            val to = LaMapper.copyFrom<DtoFrom, DtoTo>(from) {
                DtoTo::arg1 from { i }
                DtoTo::fld1 from { i }
            }
            // should use correct closure, not first one
            assertEquals("$i", to.arg1)
            assertEquals("$i", to.fld1)
        }
        assertEquals(1, LaMapper.global.cache.getMapSize() - initialSize) // expect only 1 item in cache
    }

    @Test
    internal fun test_mappers_java_caching_closure() {
        val initialSize = LaMapper.global.cache.getMapSize()
        val from = DtoFrom(5)

        for (i in 1..2) {
            val to = LaMapperJ.copyFrom(
                from,
                DtoTo::class.java,
                listOf(
                    LaMapperJ.mapFrom("arg1") { i },
                    LaMapperJ.mapFrom("fld1") { i },
                ),
            )
            // should use correct closure, not first one
            assertEquals("$i", to.arg1)
            assertEquals("$i", to.fld1)
        }

        assertEquals(1, LaMapper.global.cache.getMapSize() - initialSize) // expect only 1 item in cache
    }
}
