package lamapper.demo1

import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import kotlin.system.exitProcess

/*
 * @author Augustus
 * created on 2023-02-05
*/
fun main(args: Array<String>) {
    println("Started")
    val source = Source(f1 = "aa")

    // need do execute 1000 calls before compiling start
    for (i in 0..1000)
        source.toTarget()

    println("wait 10s for compile (please check further message)")
    println()
    Thread.sleep(10000)
    println()

    println(source.toTarget())
    println("Finished")
    exitProcess(0)
}

data class Source(
    val f1: String
)

data class Target(
    val f1: String
)

// create laMapper with enabled runtime compiling option
val laMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(tryCompile = true))

fun Source.toTarget(): Target = laMapper.copyFrom(this)
