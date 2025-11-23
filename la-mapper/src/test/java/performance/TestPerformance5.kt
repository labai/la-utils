package performance

import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.IFieldCopier
import com.github.labai.utils.reflect.LaPojo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/*
 * @author Augustus
 * created on 2025-11-23
 *
 * copyFields
 *
 * pojo=1408, asgn=135, mapr=414, part=467, refl=3174
 *
*/
class TestPerformance5 {
    private val reflectionLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(disableCompile = true))
    private val partialCompiledFactory = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(disableFullCompile = true))

    @Test
    @Disabled
    fun test_performance_copyFrom() {
        val defaultMapper = getFieldCopier(LaMapper.global)
        val reflectionMapper = getFieldCopier(reflectionLaMapper)
        val partialCompiledMapper: IFieldCopier<From, To> = getFieldCopier(partialCompiledFactory)
        val target = To()
        PerfFieldCopierHelper.testForClasses(
            createFromFn = { createFrom(it) },
            target,
            pojoFn = { fr, to -> LaPojo.copyFields(fr, to) },
            mapperFn = { fr, to -> defaultMapper.copyFields(fr, to) },
            assignFn = { fr, to -> To.copyFromFrom(fr, to) },
            partialFn = { fr, to -> partialCompiledMapper.copyFields(fr, to) },
            reflectionFn = { fr, to -> reflectionMapper.copyFields(fr, to) },
        )
    }

    private fun getFieldCopier(laMapper: LaMapper): IFieldCopier<From, To> {
        return laMapper.createAutoMapper(From::class, To::class, true)
    }

    internal fun createFrom(i: Int): From {
        return From().apply {
            a00 = null
            a01 = "a01-$i"
            a02 = "a02-$i"
            a03 = "a03-$i"
            a04 = "a04-$i"
            a05 = "a05-$i"
            a06 = "a06-$i"
            a07 = "a07-$i"
            a08 = "a08-$i"
            a09 = "a09-$i"
            a10 = "a10-$i"
            a11 = "a11-$i"
            a12 = "a12-$i"
            a13 = "a13-$i"
            a14 = "a14-$i"
            a15 = "a15-$i"
            a16 = "a16-$i"
            a17 = "a17-$i"
            a18 = "a18-$i"
            a19 = "a19-$i"
        }
    }

    class From {
        var a00: String? = null
        var a01: String? = null
        var a02: String? = null
        var a03: String? = null
        var a04: String? = null
        var a05: String? = null
        var a06: String? = null
        var a07: String? = null
        var a08: String? = null
        var a09: String? = null
        var a10: String? = null
        var a11: String? = null
        var a12: String? = null
        var a13: String? = null
        var a14: String? = null
        var a15: String? = null
        var a16: String? = null
        var a17: String? = null
        var a18: String? = null
        var a19: String? = null
    }

    // for properties assign test
    class To {
        var a00: String? = null
        var a01: String? = null
        var a02: String? = null
        var a03: String? = null
        var a04: String? = null
        var a05: String? = null
        var a06: String? = null
        var a07: String? = null
        var a08: String? = null
        var a09: String? = null
        var a10: String? = null
        var a11: String? = null
        var a12: String? = null
        var a13: String? = null
        var a14: String? = null
        var a15: String? = null
        var a16: String? = null
        var a17: String? = null
        var a18: String? = null
        var a19: String? = null

        companion object {
            fun copyFromFrom(fr: From, to: To) {
                to.a00 = fr.a00
                to.a01 = fr.a01
                to.a02 = fr.a02
                to.a03 = fr.a03
                to.a04 = fr.a04
                to.a05 = fr.a05
                to.a06 = fr.a06
                to.a07 = fr.a07
                to.a08 = fr.a08
                to.a09 = fr.a09
                to.a10 = fr.a10
                to.a11 = fr.a11
                to.a12 = fr.a12
                to.a13 = fr.a13
                to.a14 = fr.a14
                to.a15 = fr.a15
                to.a16 = fr.a16
                to.a17 = fr.a17
                to.a18 = fr.a18
                to.a19 = fr.a19
            }
        }
    }
}

internal object PerfFieldCopierHelper {

    fun <Fr, To> runCopyFields(list: List<Fr>, to: To, repeat: Int, mapFn: (fr: Fr, to: To) -> Unit): Long {
        val time = measureTimeMillis {
            for (i in 1..repeat) {
                for (fr in list) {
                    mapFn(fr, to)
                }
            }
        }
        return time
    }

    private fun <Fr, To> runForList(
        list: List<Fr>,
        to: To,
        repeat: Int,
        pojoFn: ((fr: Fr, to: To) -> Unit),
        mapperFn: (fr: Fr, to: To) -> Unit,
        assignFn: (fr: Fr, to: To) -> Unit,
        partialFn: (fr: Fr, to: To) -> Unit,
        reflectionFn: (fr: Fr, to: To) -> Unit,
    ): Map<String, Long> {
        val stats = mutableMapOf<String, Long>()
        stats["pojo"] = runCopyFields(list, to, repeat, pojoFn)
        stats["asgn"] = runCopyFields(list, to, repeat, assignFn)
        stats["mapr"] = runCopyFields(list, to, repeat, mapperFn)
        stats["part"] = runCopyFields(list, to, repeat, partialFn)
        stats["refl"] = runCopyFields(list, to, repeat, reflectionFn)
        return stats
    }

    fun <T> createList(size: Int, createFromFn: (iteration: Int) -> T): List<T> {
        val list = ArrayList<T>(size)
        for (i in 1..size) {
            list.add(createFromFn(i))
        }
        return list
    }

    fun <Fr, To> testForClasses(
        createFromFn: (iteration: Int) -> Fr,
        to: To,
        pojoFn: (fr: Fr, to: To) -> Unit,
        mapperFn: (fr: Fr, to: To) -> Unit,
        assignFn: (fr: Fr, to: To) -> Unit,
        partialFn: (fr: Fr, to: To) -> Unit,
        reflectionFn: (fr: Fr, to: To) -> Unit,
        repeatCount: Int = 100,
    ) {
        val listSize = 10000

        val list = createList(listSize, createFromFn)

        println("Warmup")
        // warmup
        runForList(list, to, repeatCount, pojoFn, mapperFn, assignFn, partialFn, reflectionFn)

        val stats = PerfHelper.Stats()
        for (i in 1..4) {
            val st = runForList(list, to, repeatCount, pojoFn, mapperFn, assignFn, partialFn, reflectionFn)
            println("Iteration $i: $st")
            stats.addStats(st)
        }

        println("Averages ${stats.getAverages()}")
    }
}
