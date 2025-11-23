package performance

import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import jtest.StructuresInJava.Record12
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/*
 * @author Augustus
 * created on 2025-05-03
 *
 * Records
 *
 *  60 ms - manual create
 *  155 ms - full compile (default mapper)
 *  750 ms - part compile
 *  1170 ms - reflection
 *
*/
class TestPerformance4 {
    private val reflectionLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(disableCompile = true))
    private val partialCompiledFactory = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(disableFullCompile = true))
    private val fullCompiledFactory = LaMapper(LaConverterRegistry.global, LaMapperConfig())

    @Test
    @Disabled
    fun test_performance_record() {
        val reflectionMapper = getMapper(reflectionLaMapper)
        val partialCompiledMapper: AutoMapper<Record12, Record12> = getMapper(partialCompiledFactory)
        val fullCompiledMapper: AutoMapper<Record12, Record12> = getMapper(fullCompiledFactory)

        PerfHelper.testForClasses(
            createFromFn = { createFrom(it) },
            createToFn = null,
            mapperFn = { fr -> fullCompiledMapper.transform(fr) /*LaMapper.copyFrom(fr)*/ },
            assignFn = { fr -> Record12(fr.v01, fr.v02, fr.v03, fr.v04, fr.v06) },
            partialFn = { fr -> partialCompiledMapper.transform(fr) },
            reflectionFn = { fr -> reflectionMapper.transform(fr) },
            repeatCount = 500,
        )
    }

    private fun getMapper(laMapper: LaMapper): AutoMapper<Record12, Record12> {
        return laMapper.autoMapper()
    }

    private fun createFrom(i: Int): Record12 {
        return Record12(
            i * 10 + 1L,
            i * 10 + 2L,
            i * 10 + 3,
            i * 10 + 4,
            "$i-6",
        )
    }
}
