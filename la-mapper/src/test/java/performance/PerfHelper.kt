package performance

import com.github.labai.deci.Deci
import com.github.labai.deci.deci
import com.github.labai.utils.reflect.LaPojo
import kotlin.system.measureTimeMillis

/**
 * @author Augustus
 *         created on 2023.01.28
 */
internal object PerfHelper {

    class Stats : HashMap<String, MutableList<Long>>() {
        fun addStats(st: Map<String, Long>) {
            for (n in st) {
                if (n.key !in this) {
                    this[n.key] = mutableListOf()
                }
                this[n.key]!!.add(n.value)
            }
        }

        fun getAverages(): Map<String, Deci> {
            return this.map { it.key to it.value.average().toBigDecimal().deci.round(0) }.toMap()
        }
    }

    fun <Fr, To> runPojoCopy(list: List<Fr>, repeat: Int, createToFn: (fr: Fr) -> To): Long {
        val time = measureTimeMillis {
            val res = mutableListOf<To>()
            for (i in 1..repeat) {
                for (fr in list) {
                    val to = createToFn(fr)
                    LaPojo.copyFields(fr, to)
                    res.add(to)
                }
            }
        }
        return time
    }

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    fun <Fr, To> runMapper(list: List<Fr>, repeat: Int, mapFn: (fr: Fr) -> To): Long {
        var res = listOf<To>()
        val time = measureTimeMillis {
            for (i in 1..repeat) {
                res = list.map(mapFn)
            }
        }
        return time
    }

    fun <Fr, To> runAssign(list: List<Fr>, repeat: Int, mapFn: (fr: Fr) -> To): Long {
        val time = measureTimeMillis {
            for (i in 1..repeat) {
                val res = mutableListOf<To>()
                for (fr in list) {
                    res.add(mapFn(fr))
                }
            }
        }
        return time
    }

    private fun <Fr, To> runForList(
        list: List<Fr>,
        repeat: Int,
        createToFn: ((fr: Fr) -> To)?,
        mapperFn: (fr: Fr) -> To,
        assignFn: (fr: Fr) -> To,
        partialFn: (fr: Fr) -> To,
        reflectionFn: (fr: Fr) -> To,
    ): Map<String, Long> {
        val stats = mutableMapOf<String, Long>()
        stats["pojo"] = if (createToFn != null) runPojoCopy(list, repeat, createToFn) else 0
        stats["asgn"] = runAssign(list, repeat, assignFn)
        stats["mapr"] = runMapper(list, repeat, mapperFn)
        stats["part"] = runMapper(list, repeat, partialFn)
        stats["refl"] = runMapper(list, repeat, reflectionFn)
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
        createToFn: ((fr: Fr) -> To)?,
        mapperFn: (fr: Fr) -> To,
        assignFn: (fr: Fr) -> To,
        partialFn: (fr: Fr) -> To,
        reflectionFn: (fr: Fr) -> To,
        repeatCount: Int = 100,
    ) {
        val listSize = 10000

        val list = createList(listSize, createFromFn)

        println("Warmup")
        // warmup
        runForList(list, repeatCount, createToFn, mapperFn, assignFn, partialFn, reflectionFn)

        val stats = Stats()
        for (i in 1..4) {
            val st = runForList(list, repeatCount, createToFn, mapperFn, assignFn, partialFn, reflectionFn)
            println("Iteration $i: $st")
            stats.addStats(st)
        }

        println("Averages ${stats.getAverages()}")
    }
}
