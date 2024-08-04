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
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.KType

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
    internal val laMapperAsmCompiler2 = LaMapperAsmCompiler2(serviceContext)
    internal val laMapperAsmCompiler3 = LaMapperAsmCompiler3(serviceContext)

    internal inner class AutoMapperImpl<Fr : Any, To : Any>(
        private val sourceType: KClass<Fr>,
        private val targetType: KClass<To>,
        private var manualMappings: Map<String, IMappingBuilderItem<Fr>> = mapOf(),
    ) : AutoMapper<Fr, To> {

        private lateinit var activeMapper: AutoMapper<Fr, To>
        internal lateinit var struct: MappedStruct<Fr, To>
        private val isInitialized = AtomicBoolean(false)

        private var needToCompile = !config.disableCompile
        private var counter: Int = 0

        internal fun init() {
            if (!isInitialized.get()) {
                synchronized(this) {
                    if (!isInitialized.get()) {
                        struct = MappedStruct(
                            sourceType,
                            targetType,
                            manualMappings.filter { it.value is PropMapping }.mapValues { it.value as PropMapping },
                            manualMappings.filter { it.value is LambdaMapping }.mapValues { it.value as LambdaMapping },
                            serviceContext,
                        )
                        activeMapper = ReflectionAutoMapper(struct, serviceContext)
                        manualMappings = mapOf() // cleanup
                    }
                }
                isInitialized.set(true)
            }

            if (needToCompile && ++counter > config.startCompileAfterIterations) {
                synchronized(this) {
                    if (needToCompile) {
                        if (struct.targetType.java.isRecord) {
                            LaMapper.logger.trace("Target $targetType is a record, don't compile")
                        } else {
                            val hasValueClass = struct.paramBinds.firstOrNull { (it.param.type.classifier as KClass<*>).isValue }
                                ?: struct.paramBinds.firstOrNull { it.sourcePropRd?.klass?.isValue == true }
                                ?: struct.propAutoBinds.firstOrNull { it.sourcePropRd.klass.isValue || it.targetPropWr.klass.isValue }
                                ?: struct.propManualBinds.firstOrNull { it.targetPropWr.klass.isValue }
                            try {
                                activeMapper = if (hasValueClass != null || config.disableSyntheticConstructorCall || config.disableFullCompile) {
                                    LaMapper.logger.trace("Use partial compile for $sourceType to $targetType mapper")
                                    laMapperAsmCompiler2.compiledMapper(struct)
                                } else {
                                    LaMapper.logger.trace("Use full compile for $sourceType to $targetType mapper")
                                    laMapperAsmCompiler3.compiledMapper(struct)
                                }
                            } catch (e: Exception) {
                                if (config.failOnOptimizationError)
                                    throw e
                                LaMapper.logger.debug("Failed to compile mapper $sourceType to $targetType, use reflection mode. Error: ${e.message}")
                            }
                        }
                        needToCompile = false
                    }
                }
            }
        }

        override fun transform(from: Fr): To {
            init()
            return activeMapper.transform(from)
        }
    }

    internal interface IMappingBuilderItem<Fr>

    internal class LambdaMapping<Fr>(
        val mapper: ManualFn<Fr>,
        val sourceType: KType?, // type of lambda return
    ) : IMappingBuilderItem<Fr> {
        internal var convNnFn: ConvFn? = null
        internal var targetType: KType? = null
    }

    internal class PropMapping<Fr>(
        val frName: String,
    ) : IMappingBuilderItem<Fr>
}

internal class ClassTrioMap<T> {
    private val map: MutableMap<ClassTrio, Pair<ClassTrio, T>> = ConcurrentHashMap()

    private data class ClassTrio(val source: KClass<*>, val target: KClass<*>, val mapper: Any?)
    private var last: Pair<ClassTrio, T>? = null

    fun <Fr : Any, To : Any> getOrPut(sourceType: KClass<Fr>, targetType: KClass<To>, mapperClass: KClass<*>?, itemFn: Supplier<T>): T {
        return getOrPutImpl(sourceType, targetType, mapperClass, itemFn)
    }

    fun <Fr : Any, To : Any> getOrPut(sourceType: KClass<Fr>, targetType: KClass<To>, mapperRef: List<*>, itemFn: Supplier<T>): T {
        return getOrPutImpl(sourceType, targetType, mapperRef, itemFn)
    }

    private fun <Fr : Any, To : Any> getOrPutImpl(sourceType: KClass<Fr>, targetType: KClass<To>, mapperRef: Any?, itemFn: Supplier<T>): T {
        val last = last
        if (last != null && last.first.source === sourceType && last.first.target === targetType && last.first.mapper === mapperRef) {
            return last.second
        }
        val key = ClassTrio(sourceType, targetType, mapperRef)
        val value = map[key]
        if (value != null) {
            this.last = value
            return value.second
        }
        synchronized(map) {
            return map.computeIfAbsent(key) { key to itemFn.get() }.second
        }
    }

    operator fun <Fr : Any, To : Any> get(sourceType: KClass<Fr>, targetType: KClass<To>, mapperClass: KClass<*>?): T? {
        return map[ClassTrio(sourceType, targetType, mapperClass)]?.second
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

internal fun ConvFn?.convertValNn(v: Any?): Any? = if (this == null) v else this.convert(v)
internal fun ConvFn?.convertValOrNull(v: Any?): Any? = if (v == null) null else (if (this == null) v else this.convert(v))
