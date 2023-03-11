import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.LaMapper.MappingBuilder
import java.util.stream.Stream

/*
 * @author Augustus
 * created on 2023-03-11
*/
internal object MappersConfig {
    internal const val ENGINES = "MappersConfig#engines"

    @JvmStatic
    fun engines(): Stream<String>? {
        return Stream.of("refl_nosynth", "refl_synth", "part_compile", "part_nosynth", "full_compile")
    }

    private val defaultFactory = LaMapper(
        LaConverterRegistry.global,
        LaMapperConfig().copy(failOnOptimizationError = true, startCompileAfterIterations = 0),
    )

    private val reflectionSynthFactory = LaMapper(
        LaConverterRegistry.global,
        LaMapperConfig().copy(disableCompile = true, disableSyntheticConstructorCall = false, startCompileAfterIterations = 0, failOnOptimizationError = true),
    )

    private val reflectionNoSynthFactory = LaMapper(
        LaConverterRegistry.global,
        LaMapperConfig().copy(disableCompile = true, disableSyntheticConstructorCall = true, startCompileAfterIterations = 0, failOnOptimizationError = true),
    )

    private val partCompileSynthFactory = LaMapper(
        LaConverterRegistry.global,
        LaMapperConfig().copy(disableCompile = false, disableFullCompile = true, disableSyntheticConstructorCall = false, startCompileAfterIterations = 0, failOnOptimizationError = true),
    )

    private val partCompileNoSynthFactory = LaMapper(
        LaConverterRegistry.global,
        LaMapperConfig().copy(disableCompile = false, disableFullCompile = true, disableSyntheticConstructorCall = true, startCompileAfterIterations = 0, failOnOptimizationError = true),
    )

    internal inline fun <reified Fr : Any, reified To : Any> getMapper(
        engine: String,
        noinline mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
    ): AutoMapper<Fr, To> {
        return when (engine) {
            // pure reflection mapper (no compile, disabled kotlin synth constructor)
            "refl_nosynth" -> {
                reflectionNoSynthFactory.autoMapper(mapping)
            }

            // reflection mapper, kotlin synth constructor enabled
            "refl_synth" -> {
                reflectionSynthFactory.autoMapper(mapping)
            }

            // partial compile, kotlin synth constructor enabled
            "part_compile" -> {
                partCompileSynthFactory.autoMapper(mapping)
            }

            // partial compile, kotlin synth constructor disabled
            "part_nosynth" -> {
                partCompileNoSynthFactory.autoMapper(mapping)
            }

            // default - fully compiled
            "full_compile" -> {
                defaultFactory.autoMapper(mapping)
            }

            else -> throw IllegalArgumentException("Invalid mapper type: $engine")
        }
    }
}
