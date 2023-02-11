/*
The MIT License (MIT)

Copyright (c) 2022 Augustus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.github.labai.utils.mapper

import com.github.labai.utils.convert.ITypeConverter
import com.github.labai.utils.mapper.KotlinObjectSourceGenerator.GeneratedSource
import com.github.labai.utils.mapper.LaMapper.AutoMapperImpl
import com.github.labai.utils.mapper.LaMapper.ConverterConfig
import com.github.labai.utils.mapper.MapperCompiler.DynamicConverter
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.SECONDS
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmName

/**
 * @author Augustus
 *         created on 2023.01.25
 *
 * try to compile mapping object.
 *
 * for la-mapper only!
 */
class MapperCompiler(laMapper: LaMapper) {
    private val logger = LoggerFactory.getLogger(MapperCompiler::class.java)
    private val sourceLogger = LoggerFactory.getLogger("LaMapper-generatedSource")

    class DynamicConverter(laMapper: LaMapper) {
        private val dataConverters: DataConverters = laMapper.dataConverters
        fun convertValue(value: Any?, targetKlass: KClass<*>): Any? {
            return dataConverters.convertValue(value, targetKlass)
        }
    }

    private val dynamicConverter = DynamicConverter(laMapper)
    private val config: ConverterConfig = laMapper.config

    internal fun <Fr : Any, To : Any> compiledMapper(mapper: AutoMapperImpl<Fr, To>): AutoMapper<Fr, To>? {
        mapper.init()
        return compiledMapper(mapper.struct)
    }

    internal fun <Fr : Any, To : Any> compiledMapper(struct: MappedStruct<Fr, To>): AutoMapper<Fr, To>? {
        val startTs = System.currentTimeMillis()

        val generator = KotlinObjectSourceGenerator(dynamicConverter, config, struct)

        val generated: GeneratedSource = generator.generate()

        var obj: AutoMapper<Fr, To>? = null
        try {
            obj = processCompile(generated)
            logger.debug("Compiled mapper from '${struct.sourceType.qualifiedName}' to '${struct.targetType.qualifiedName}', time=${System.currentTimeMillis() - startTs}ms")
            sourceLogger.trace(generated.source)
        } catch (e: Throwable) {
            logger.info("Failed to compile mapper from '${struct.sourceType.qualifiedName}' to '${struct.targetType.qualifiedName}', continue to use reflection mapping: ${e.message}")
            sourceLogger.debug(generated.source)
        }
        return obj
    }

    private fun <R> processCompile(generatedSource: GeneratedSource): R {
        val scriptEngineManager = ScriptEngineManager()

        val scriptEngine = try {
            scriptEngineManager.getEngineByExtension("kts")
        } catch (e: Exception) {
            throw RuntimeException("Script engine for extension 'kts' was not found: ${e.message}")
        } ?: throw RuntimeException("Script engine for extension 'kts' was not found")

        val engine: ScriptEngine = try {
            val factory: ScriptEngineFactory = scriptEngine.factory // JVM knows, that kts extension should be compiled with KotlinJsr223JvmLocalScriptEngineFactory
            factory.scriptEngine
        } catch (e: Exception) {
            throw RuntimeException("Can't get scriptEngine: ${e.message}")
        }

        val engineBindings = engine.createBindings()
        for ((key, obj) in generatedSource.bindings) {
            engineBindings[key] = obj
        }

        @Suppress("UNCHECKED_CAST")
        return engine.eval(generatedSource.source, engineBindings) as R
    }
}

// generate kotlin sources for compile for mapping function
//
internal class KotlinObjectSourceGenerator<Fr : Any, To : Any>(
    private val dynamicConverter: DynamicConverter,
    private val converterConfig: ConverterConfig,
    private val struct: MappedStruct<Fr, To>,
) {

    // work variables
    private val convFnArr = ArrayList<ConvFn>()
    private val manualMapperArr = ArrayList<ManualFn<Fr>>()

    internal class GeneratedSource(
        val source: String,
        val bindings: Map<String, Any>,
    )

    fun generate(): GeneratedSource {

        val autoAssigns = generatePropAssign()
        val manualAssigns = generateManualAssign()
        val constrArgs = generateConstrArgs()

        val sourceKlassName = checkNotNull(struct.sourceType.qualifiedName) { "invalid class ${struct.sourceType.simpleName} (${struct.sourceType.jvmName})" }
        val targetKlassName = checkNotNull(struct.targetType.qualifiedName) { "invalid class ${struct.targetType.simpleName} (${struct.targetType.jvmName})" }

        @Language("kotlin")
        val src = """
            import $sourceKlassName as SrcType
            import $targetKlassName as TrgType
            object : ${AutoMapper::class.qualifiedName}<SrcType, TrgType> {
                private val convFnArr = bindings["convFnArr"] as ArrayList<${ITypeConverter::class.qualifiedName}<in Any, out Any?>>
                private val manualMapperArr = bindings["manualMapperArr"] as ArrayList<(SrcType) -> Any?>
                private val dynamicConverter = bindings["dynamicConverter"] as ${DynamicConverter::class.qualifiedName}
                override fun transform(fr: SrcType): TrgType {
                    val to = TrgType(
                        ${constrArgs.prependIndent("                        ").trim()}
                    )
                    ${autoAssigns.prependIndent("                    ").trim()}
                    ${manualAssigns.prependIndent("                    ").trim()}
                    return to
                }
            }
        """.trimIndent()

        val bindings = mapOf(
            "convFnArr" to convFnArr,
            "manualMapperArr" to manualMapperArr,
            "dynamicConverter" to dynamicConverter,
        )
        return GeneratedSource(src, bindings)
    }

    private fun generatePropAssign(): String {
        var res = ""
        for (propMap in struct.propAutoMappers) {
            var s = ""
            var wasConverted = false
            if (propMap.convFn == null || propMap.convFn == DataConverters.noConvertConverter) {
                s += "to.${propMap.targetProp.name} = fr.${propMap.sourceProp.name}"
            } else {
                convFnArr.add(propMap.convFn)
                s += "to.${propMap.targetProp.name} = convFnArr[${convFnArr.size - 1}].convert(fr.${propMap.sourceProp.name})"
                wasConverted = true
            }
            s += addAsTypeAndElvis(propMap.targetProp.returnType, wasConverted)
            res += s + "\n"
        }
        return res
    }

    private fun generateManualAssign(): String {
        var res = ""
        for (mm in struct.propManualMappers) {
            manualMapperArr.add(mm.manualMapper.mapper)
            var s = "manualMapperArr[${manualMapperArr.size - 1}](fr)"
            if (mm.manualMapper.convFn != null && mm.manualMapper.convFn != DataConverters.noConvertConverter) {
                convFnArr.add(mm.manualMapper.convFn!!)
                s = "convFnArr[${convFnArr.size - 1}].convert($s)"
            } else if (mm.manualMapper.sourceType == null) {
                // convert on the fly
                s = "dynamicConverter.convertValue($s, ${getKTypeClassString(mm.targetProp.returnType)})"
            }
            s += addAsTypeAndElvis(mm.targetProp.returnType, true)

            res += "to.${mm.targetProp.name} = $s"
            res += "\n"
        }
        return res
    }

    private fun generateConstrArgs(): String {
        var paramStr = ""
        for (pm in struct.paramMappers) {
            var wasConverted = false
            var s: String
            if (pm.manualMapper != null) {
                manualMapperArr.add(pm.manualMapper.mapper)
                s = "manualMapperArr[${manualMapperArr.size - 1}](fr)"
                wasConverted = true
            } else {
                s = "fr.${pm.sourceProp?.name}"
            }

            if (pm.convFn != null && pm.convFn != DataConverters.noConvertConverter) {
                convFnArr.add(pm.convFn)
                s = "convFnArr[${convFnArr.size - 1}].convert($s)"
                wasConverted = true
            } else if (pm.manualMapper != null && pm.manualMapper.sourceType == null) {
                // convert on the fly
                s = "dynamicConverter.convertValue($s, ${getKTypeClassString(pm.param.type)})"
                wasConverted = true
            }
            s += addAsTypeAndElvis(pm.param.type, wasConverted)

            paramStr += "${pm.param.name} = $s"
            paramStr += ",\n"

        }
        return paramStr
    }

    // add 'as type', e.g.:
    //  ' as kotlin.Int? ?: 0'
    private fun addAsTypeAndElvis(targetType: KType, addAsType: Boolean): String {
        var s = ""
        val elvis = addElvis(targetType)
        if (addAsType) {
            s += " as $targetType"
            if (!targetType.isMarkedNullable && elvis.isNotBlank())
                s += "?" // force 'as type' to be nullable, as later will be changed to non-nullable by elvis operator
        }
        return s + elvis
    }

    private fun addElvis(targetType: KType): String {
        if (targetType.isMarkedNullable)
            return ""
        val klass = targetType.classifier
        if (klass !is KClass<*>)
            return ""
        val nl = getPrimitiveDefault(klass) ?: return ""
        return " ?: $nl"
    }

    private fun getPrimitiveDefault(klass: KClass<*>): String? {
        if (klass.java == String::class.java)
            return if (converterConfig.autoConvertNullToString) "\"\"" else null
        if (!converterConfig.autoConvertNullForPrimitive)
            return null
        return when (klass) {
            Boolean::class -> "false"
            Char::class -> "'\u0000'"
            Byte::class -> "0"
            UByte::class -> "0"
            Short::class -> "0"
            UShort::class -> "0"
            Int::class -> "0"
            UInt::class -> "0"
            Long::class -> "0L"
            ULong::class -> "0L"
            Float::class -> "0.0f"
            Double::class -> "0.0"
            else -> null
        }
    }

    private fun getKTypeClassString(kType: KType) = (kType.classifier as KClass<*>).qualifiedName + "::class"
}

// task queue with 2 workers for object compiler
//
internal object CompilerQueue {
    private val executor = createExecutor(threadPrefix = "LaMapCmp", minCount = 0, maxCount = 2, queueSize = 999)

    internal fun <Fr : Any, To : Any> addTask(compiler: MapperCompiler, mappedStruct: MappedStruct<Fr, To>, callbackFn: (AutoMapper<Fr, To>?) -> Unit) {
        executor.submit {
            val res: AutoMapper<Fr, To>? = compiler.compiledMapper(mappedStruct)
            callbackFn(res)
        }
    }

    private fun createExecutor(threadPrefix: String, minCount: Int, maxCount: Int, queueSize: Int): ThreadPoolExecutor {
        val threadFactory: ThreadFactory = object : ThreadFactory {
            private var counter = 0
            override fun newThread(runnable: Runnable): Thread {
                return Thread(runnable, threadPrefix + "-" + ++counter)
            }
        }
        return ThreadPoolExecutor(
            minCount,
            maxCount,
            30, SECONDS,
            LinkedBlockingQueue(queueSize),
            threadFactory
        )
    }
}
