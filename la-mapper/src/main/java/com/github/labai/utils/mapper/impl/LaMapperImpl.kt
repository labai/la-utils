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
package com.github.labai.utils.mapper.impl

import com.github.labai.utils.convert.IConverterResolver
import com.github.labai.utils.convert.ITypeConverter
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.ILaMapperConfig
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.MappedStruct.ParamBind
import com.github.labai.utils.mapper.impl.SynthConstructorUtils.SynthConConf
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance

/**
 * @author Augustus
 *         created on 2022.11.15
 *
 * https://github.com/labai/la-utils/tree/main/la-mapper
 *
 * implementation
 */
internal class LaMapperImpl(
    laConverterRegistry: IConverterResolver,
    _config: ILaMapperConfig,
) {

    private val config: LaMapperConfig = if (_config is LaMapperConfig) {
        _config
    } else {
        // "soft" binding, to avoid problems between versions
        // save to use global - it is already created as it has _config=LaMapperConfig
        LaMapper.global.copyFrom(_config, _config.javaClass.kotlin, LaMapperConfig::class, null)
    }
    internal val dataConverters = DataConverters(laConverterRegistry, config)
    internal val serviceContext = ServiceContext().apply { this.config = this@LaMapperImpl.config; this.dataConverters = this@LaMapperImpl.dataConverters }
    internal val laMapperScriptCompiler = LaMapperScriptCompiler(serviceContext)
    internal val laMapperAsmCompiler = LaMapperAsmCompiler(serviceContext)

    internal inner class AutoMapperImpl<Fr : Any, To : Any>(
        private val sourceType: KClass<Fr>,
        private val targetType: KClass<To>,
        private var manualMappings: Map<String, ManualMapping<Fr>> = mapOf(),
    ) : AutoMapper<Fr, To> {

        internal lateinit var struct: MappedStruct<Fr, To>
        private val isInitialized = AtomicBoolean(false)

        private var needToCompile = config.tryScriptCompile || config.partiallyCompile
        private var counter: Int = 0
        private var simpleReflectionAutoMapper: AutoMapper<Fr, To>? = null
        private var compiledScriptMapper: AutoMapper<Fr, To>? = null
        private var compiledAsmMapper: AutoMapper<Fr, To>? = null

        internal fun init() {
            if (isInitialized.compareAndSet(false, true)) {
                struct = MappedStruct(sourceType, targetType, manualMappings, serviceContext)
                simpleReflectionAutoMapper = SimpleReflectionAutoMapper(struct, serviceContext)
                manualMappings = mapOf() // cleanup
            }

            if (needToCompile && ++counter > config.startCompileAfterIterations) {
                synchronized(this) {
                    if (needToCompile) {
                        if (config.tryScriptCompile) {
                            CompilerQueue.addTask(laMapperScriptCompiler, this.struct) { compiled ->
                                if (compiled != null) {
                                    compiledScriptMapper = compiled
                                }
                            }
                        } else if (config.partiallyCompile) {
                            compiledAsmMapper = laMapperAsmCompiler.compiledMapper(struct)
                            simpleReflectionAutoMapper = null // doesn't need anymore, cleanup
                        }
                        needToCompile = false
                    }
                }
            }
        }

        override fun transform(from: Fr): To {
            init()

            if (compiledScriptMapper != null) {
                return compiledScriptMapper!!.transform(from)
            }

            if (compiledAsmMapper != null) {
                return compiledAsmMapper!!.transform(from)
            }

            if (simpleReflectionAutoMapper != null) {
                return simpleReflectionAutoMapper!!.transform(from)
            }

            throw IllegalStateException("Internal autoMapper is not initialized")
        }
    }

    internal class ManualMapping<Fr>(
        val mapper: ManualFn<Fr>,
        val sourceType: KType?,
    ) {
        internal var convFn: ConvFn? = null
        internal var targetType: KType? = null
    }
}

// default reflection based transformer
private class SimpleReflectionAutoMapper<Fr : Any, To : Any>(
    private val struct: MappedStruct<Fr, To>,
    serviceContext: ServiceContext,
) : AutoMapper<Fr, To> {
    private val objectCreator: ObjectCreator<Fr, To> = ObjectCreator(struct.targetType, struct.targetConstructor, struct.paramBinds, serviceContext.config)
    private val dataConverters = serviceContext.dataConverters

    override fun transform(from: Fr): To {
        val target: To = objectCreator.createObject(from)

        // ordinary (non-constructor) fields, auto mapped
        var i = -1
        var size = struct.propAutoBinds.size
        while (++i < size) {
            val propMapper = struct.propAutoBinds[i]
            val valTo = propMapper.sourcePropRd.getValue(from)
            val valConv = propMapper.convFn.convertValOrNull(valTo) ?: dataConverters.convertNull(propMapper.targetPropWr.returnType)
            propMapper.targetPropWr.setValue(target, valConv)
        }

        // ordinary (non-constructor) fields, manually mapped
        i = -1
        size = struct.propManualBinds.size
        while (++i < size) {
            val mapr = struct.propManualBinds[i]
            val valTo = mapr.manualMapping.mapper.invoke(from)
            var valConv = if (valTo == null) {
                null
            } else if (mapr.manualMapping.sourceType == null) {
                dataConverters.convertValue(valTo, mapr.targetPropWr.klass)
            } else {
                mapr.manualMapping.convFn.convertValOrNull(valTo)
            }
            if (valConv == null)
                valConv = dataConverters.convertNull(mapr.targetPropWr.returnType)
            mapr.targetPropWr.setValue(target, valConv)
        }
        return target
    }
}

// create object 'targetType' by paramMapper
// with some optimization based on constructor type
internal open class ObjectCreator<Fr : Any, To : Any>(
    private val targetType: KClass<To>,
    private val targetConstructor: KFunction<To>?,
    private val paramBinds: Array<ParamBind<Fr>>,
    private val config: LaMapperConfig,
) {
    // for case with provided all args
    private val allArgsNullsTemplate: Array<Any?>?

    // for case with optional args, but enabled synthetic constructor call hack
    private val synthConConf: SynthConConf<Fr, To>?
    private var disableSynthCall = false

    init {
        if (targetConstructor != null && paramBinds.size == targetConstructor.parameters.size) {
            allArgsNullsTemplate = arrayOfNulls(targetConstructor.parameters.size)
            synthConConf = null
        } else if (targetConstructor != null && !config.disableSyntheticConstructorCall) {
            allArgsNullsTemplate = null
            synthConConf = SynthConstructorUtils.prepareSynthConParams(targetType, paramBinds)
        } else {
            allArgsNullsTemplate = null
            synthConConf = null
        }
    }

    open fun createObject(from: Fr): To {
        val target: To = if (targetConstructor == null) {
            targetType.createInstance()
        } else if (allArgsNullsTemplate != null) {
            // args as array are slightly faster
            val paramArr = if (allArgsNullsTemplate.isNotEmpty()) allArgsNullsTemplate.clone() else MappedStruct.EMPTY_ARRAY
            var i = -1
            val size = paramBinds.size
            while (++i < size) {
                paramArr[i] = paramBinds[i].mapParam(from)
            }
            targetConstructor.call(*paramArr)
        } else {
            // with optional args
            if (synthConConf != null && !disableSynthCall) {
                val paramArr = synthConConf.synthArgsTemplate.clone()
                var i = -1
                val size = paramBinds.size
                while (++i < size) {
                    val param = synthConConf.paramBindWithHoles[i] ?: continue
                    paramArr[i] = param.mapParam(from)
                }
                try {
                    synthConConf.synthConstructor.newInstance(*paramArr)
                } catch (e: IllegalArgumentException) {
                    if (!config.failOnOptimizationError && e.stackTrace.isNotEmpty() && e.stackTrace[0].className.startsWith("sun.reflect.")) { // ensure, it is invocation problem
                        LaMapper.logger.debug("Failed call $targetType (args: ${paramArr.joinToString(", ")}; params: ${synthConConf.synthConstructor.parameters.joinToString(", ")}")
                        disableSynthCall = true
                        // retry with native kotlin call
                        return createObject(from)
                    }
                    LaMapper.logger.debug("Failed call $targetType (args: ${paramArr.joinToString(", ")}; params: ${synthConConf.synthConstructor.parameters.joinToString(", ")}")
                    throw e
                }
            } else {
                val params = paramBinds.associate {
                    it.param to it.mapParam(from)
                }
                targetConstructor.callBy(params)
            }
        }
        return target
    }
}

internal class ClassTrioMap<T> {
    private val map: MutableMap<ClassTrio, T> = ConcurrentHashMap()

    private data class ClassTrio(val source: KClass<*>, val target: KClass<*>, val mapper: KClass<*>?)

    fun <Fr : Any, To : Any> getOrPut(sourceType: KClass<Fr>, targetType: KClass<To>, mapperClass: KClass<*>?, itemFn: Supplier<T>): T {
        val key = ClassTrio(sourceType, targetType, mapperClass)
        val value = map[key]
        if (value != null) return value
        synchronized(map) {
            return map.computeIfAbsent(key) { itemFn.get() }
        }
    }

    operator fun <Fr : Any, To : Any> get(sourceType: KClass<Fr>, targetType: KClass<To>, mapperClass: KClass<*>?): T? {
        return map[ClassTrio(sourceType, targetType, mapperClass)]
    }

    @TestOnly
    internal fun getMapSize() = map.size
}

// use to pass as parameter to MapperCompile (public, but not public)
class ServiceContext {
    internal lateinit var config: LaMapperConfig
    internal lateinit var dataConverters: DataConverters
}

// data type converter (from la-converter)
internal typealias ConvFn = ITypeConverter<in Any, out Any?>

// manual mapping lambda
internal typealias ManualFn<Fr> = (Fr) -> Any?

internal fun ConvFn?.convertValOrNull(v: Any?): Any? = if (v == null) null else (if (this == null) v else this.convert(v))
