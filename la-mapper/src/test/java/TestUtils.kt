import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.AutoMapperImpl
import com.github.labai.utils.mapper.LaMapper.ConverterConfig
import com.github.labai.utils.mapper.LaMapper.MapperBuilder
import com.github.labai.utils.mapper.MapperCompiler
import com.google.gson.GsonBuilder

/*
 * @author Augustus
 * created on 2023-02-18
*/
private val reflectionLaMapper = LaMapper(LaConverterRegistry.global, ConverterConfig().copy(partiallyCompile = false))
private val gson = GsonBuilder().setPrettyPrinting().create()

internal fun printJson(obj: Any) {
    println(gson.toJson(obj))
}

internal inline fun <reified Fr : Any, reified To : Any> getMapper(
    engine: String,
    noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null,
): AutoMapper<Fr, To> {
    return when (engine) {
        "reflect" -> {
            reflectionLaMapper.autoMapper(mapping)
        }
        "compile" -> {
            val mapperx = LaMapper.autoMapper(mapping)
            MapperCompiler(LaMapper.global).compiledMapper(mapperx as AutoMapperImpl)!!
        }
        "default" -> {
            LaMapper.autoMapper(mapping)
        }
        else -> throw IllegalArgumentException("Invalid mapper type")
    }
}
