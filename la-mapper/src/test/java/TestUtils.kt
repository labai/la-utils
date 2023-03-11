import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.LaMapper.MappingBuilder
import com.google.gson.GsonBuilder

/*
 * @author Augustus
 * created on 2023-02-18
*/

private val defaultLaMapper = LaMapper(
    LaConverterRegistry.global,
    LaMapperConfig().copy(failOnOptimizationError = true, startCompileAfterIterations = 0),
)
private val reflectionLaMapper = LaMapper(
    LaConverterRegistry.global,
    LaMapperConfig().copy(disableCompile = true, disableSyntheticConstructorCall = true, startCompileAfterIterations = 0, failOnOptimizationError = true),
)
private val noSynthConLaMapper = LaMapper(
    LaConverterRegistry.global,
    LaMapperConfig().copy(disableCompile = true, disableSyntheticConstructorCall = true, startCompileAfterIterations = 0, failOnOptimizationError = true),
)
private val gson = GsonBuilder().setPrettyPrinting().create()

internal fun printJson(obj: Any) {
    println(gson.toJson(obj))
}

internal inline fun <reified Fr : Any, reified To : Any> getMapper(
    engine: String,
    noinline mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
): AutoMapper<Fr, To> {
    return when (engine) {
        "reflect" -> {
            reflectionLaMapper.autoMapper(mapping)
        }

        "nosynth" -> {
            noSynthConLaMapper.autoMapper(mapping)
        }

        "default" -> {
            defaultLaMapper.autoMapper(mapping)
        }

        else -> throw IllegalArgumentException("Invalid mapper type")
    }
}
