package performance

import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import performance.PerfHelper.Stats
import performance.PerfHelper.createList
import performance.PerfHelper.runMapper

/*
 * @author Augustus
 * created on 2025-04-20
 *
 * Closures
 *
 * - 988 ms - lambda w/o closure (but no compile)
 * - 3032 ms - lambda with closures
 * - 20605 ms - if to create new automapper each time
 *
*/
class TestPerformance3 {
    private val reflectionFactory = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(disableCompile = true, disableSyntheticConstructorCall = true))

    class From {
        var aaa = "123"
        var bbb: Int = 4
        var a00: String? = "a00"
        var a01: String? = "a01"
        var a02: String? = "a02"
        var a03: String? = "a03"
        var a04: String? = "a04"
        var a05: String? = "a05"
    }

    class To(
        val aaa: Int,
        val arg2: String,
        val arg3: Int? = null,
    ) {
        val fld1: Int = 5
        var a00: String? = null
        var a01: String? = null
        var a02: String? = null
        var a03: String? = null
        var a04: String? = null
        var a05: String? = null
    }

    @Test
    @Disabled
    fun test_performance_closures() {
        var iCounter = 100

        val list = createList(size = 10000) { From().apply { aaa = "$it"; bbb = it } }

        runMapTest("lmbd", list) { fr ->
            reflectionFactory.copyFrom<From, To>(fr) {
                To::arg2 from { it.aaa }
                To::arg3 from { it.aaa }
                To::fld1 from { it.bbb }
            }
        }

        runMapTest("clos", list) { fr ->
            reflectionFactory.copyFrom<From, To>(fr) {
                To::arg2 from { iCounter++ }
                To::arg3 from { iCounter++ }
                To::fld1 from { iCounter++ }
            }
        }

        runMapTest("newm", list) { fr ->
            val mapper = reflectionFactory.autoMapper<From, To> {
                To::arg2 from { iCounter++ }
                To::arg3 from { iCounter++ }
                To::fld1 from { iCounter++ }
            }
            mapper.transform(fr)
        }
    }

    private fun <Fr, To> runMapTest(testType: String, list: List<Fr>, mapperFn: (fr: Fr) -> To) {
        val repeatCount = 100

        println("Warmup")
        // warmup
        runMapper(list, repeatCount, mapperFn)

        val stats = Stats()
        for (i in 1..4) {
            val time = runMapper(list, repeatCount, mapperFn)
            println("Iteration $i: ${time}ms")
            stats.addStats(mapOf(testType to time))
        }

        println("Averages ${stats.getAverages()}")
    }
}
