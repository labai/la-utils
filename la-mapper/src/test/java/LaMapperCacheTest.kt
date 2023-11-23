import com.github.labai.utils.mapper.LaMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Augustus
 *         created on 2022.12.26
 */
class LaMapperCacheTest {

    class DtoFrom(val value: Int)
    class DtoTo(val value: String)

    @Test
    internal fun test_few_mappers_caching() {
        val initialSize = LaMapper.global.cache.getMapSize()
        val from = DtoFrom(5)
        val to1 = LaMapper.copyFrom(from) {
            DtoTo::value from { "A=${it.value}" }
        }
        val to2 = LaMapper.copyFrom(from) {
            DtoTo::value from { "B=${it.value}" }
        }

        assertEquals("A=5", to1.value)
        assertEquals("B=5", to2.value)

        assertEquals(2, LaMapper.global.cache.getMapSize() - initialSize)

        for (i in 1..2) {
            LaMapper.copyFrom(from) {
                DtoTo::value from { "C=${it.value}" }
            }
        }
        assertEquals(3, LaMapper.global.cache.getMapSize() - initialSize)

        for (i in 1..2) {
            LaMapper.copyFrom<DtoFrom, DtoTo>(from)
        }
        assertEquals(4, LaMapper.global.cache.getMapSize() - initialSize)
    }
}
