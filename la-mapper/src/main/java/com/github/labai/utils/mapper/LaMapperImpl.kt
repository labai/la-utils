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

import com.github.labai.utils.convert.IConverterResolver
import com.github.labai.utils.convert.ITypeConverter
import com.github.labai.utils.hardreflect.LaHardReflect
import com.github.labai.utils.hardreflect.PropReader
import com.github.labai.utils.hardreflect.PropWriter
import com.github.labai.utils.mapper.LaMapper.ILaMapperConfig
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.LaMapperImpl.ManualMapper
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod

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

    @get:TestOnly
    internal val mapperCompiler = MapperCompiler(ServiceContext().apply { this.config = this@LaMapperImpl.config; this.dataConverters = this@LaMapperImpl.dataConverters })

    internal inner class AutoMapperImpl<Fr : Any, To : Any>(
        private val sourceType: KClass<Fr>,
        private val targetType: KClass<To>,
        private var manualMappers: Map<String, ManualMapper<Fr>> = mapOf(),
    ) : AutoMapper<Fr, To> {

        internal lateinit var struct: MappedStruct<Fr, To>
        private var allArgsNullsTemplate: Array<Any?>? = null
        private val isInitialized = AtomicBoolean(false)

        private var needToCompile = config.tryCompile
        private var counter: Int = 0
        private var compiledMapper: AutoMapper<Fr, To>? = null

        internal fun init() {
            if (isInitialized.compareAndSet(false, true)) {
                struct = MappedStruct(sourceType, targetType, manualMappers, dataConverters, config)

                if (struct.targetConstructor != null && struct.paramMappers.size == struct.targetConstructor!!.parameters.size) {
                    allArgsNullsTemplate = arrayOfNulls(struct.targetConstructor!!.parameters.size)
                }

                // cleanup
                manualMappers = mapOf()
            }
        }

        override fun transform(from: Fr): To {
            init()
            if (needToCompile && ++counter > config.startCompileAfterIterations) {
                synchronized(this) {
                    if (needToCompile) {
                        CompilerQueue.addTask(mapperCompiler, this.struct) { compiled ->
                            if (compiled != null) {
                                compiledMapper = compiled
                            }
                        }
                        needToCompile = false
                    }
                }
            }

            if (compiledMapper != null) {
                return compiledMapper!!.transform(from)
            }

            val target: To = if (struct.targetConstructor == null) {
                targetType.createInstance()
            } else if (allArgsNullsTemplate != null) {
                // args as array are slightly faster
                val paramArr = allArgsNullsTemplate!!.clone()
                var i = 0
                for (param in struct.paramMappers) {
                    paramArr[i++] = param.mapParam(from)
                }
                struct.targetConstructor!!.call(*paramArr)
            } else {
                val params = struct.paramMappers.associate {
                    it.param to it.mapParam(from)
                }
                struct.targetConstructor!!.callBy(params)
            }

            // ordinary (non-constructor) fields, auto mapped
            for (propMapper in struct.propAutoMappers) {
                val valTo = propMapper.sourceProp.getValue(from)
                val valConv = propMapper.convFn.convertVal(valTo) ?: dataConverters.convertNull(propMapper.targetProp.returnType)
                propMapper.targetProp.setValue(target, valConv)
            }

            // ordinary (non-constructor) fields, manually mapped
            for (mapr in struct.propManualMappers) {
                val valTo = mapr.manualMapper.mapper.invoke(from)
                var valConv = if (valTo == null) {
                    null
                } else if (mapr.manualMapper.sourceType == null) {
                    dataConverters.convertValue(valTo, mapr.targetProp.klass)
                } else {
                    mapr.manualMapper.convFn.convertVal(valTo)
                }
                if (valConv == null)
                    valConv = dataConverters.convertNull(mapr.targetProp.returnType)
                mapr.targetProp.setValue(target, valConv)
            }
            return target
        }
    }

    internal class ManualMapper<Fr>(
        val mapper: ManualFn<Fr>,
        val sourceType: KType?,
    ) {
        internal var convFn: ConvFn? = null
        internal var targetType: KType? = null
    }
}

internal class MappedStruct<Fr : Any, To : Any>(
    internal val sourceType: KClass<Fr>,
    internal val targetType: KClass<To>,
    private val manualMappers: Map<String, ManualMapper<Fr>> = mapOf(),
    private val dataConverters: DataConverters,
    private val config: LaMapperConfig,
) {
    internal val targetConstructor: KFunction<To>? = targetType.primaryConstructor
    internal lateinit var paramMappers: List<ParamMapper<Fr>>
    internal lateinit var propAutoMappers: List<PropAutoMapper<Fr, To>>
    internal lateinit var propManualMappers: List<PropManualMapper<Fr, To>>

    init {
        init()
    }

    internal inner class ParamMapper<Fr>(
        val param: KParameter,
        val manualMapper: ManualMapper<Fr>?,
        val sourceProp: PropertyReader<Fr>?,
        val convFn: ConvFn?,
    ) {
        private val paramKlass = param.type.classifier as KClass<*>
        internal fun mapParam(from: Fr): Any? {
            val value = if (sourceProp != null) {
                sourceProp.getValue(from)
            } else if (manualMapper != null) {
                manualMapper.mapper.invoke(from)
            } else {
                throw NullPointerException("ParamMapper must have manualMapper or sourceProp not null")
            }
            return if (manualMapper != null && manualMapper.sourceType == null) { // lambdas with unknown return type - convert based on return result
                dataConverters.convertValue(value, paramKlass)
            } else {
                convFn.convertVal(value)
            } ?: dataConverters.convertNull(param.type)
        }
    }

    internal abstract class PropertyReader<T>(
        val name: String,
    ) {
        protected abstract val returnType: KType
        val klass: KClass<*>
            get() = returnType.classifier as KClass<*>

        abstract fun getValue(pojo: T): Any?
    }

    internal class PropertyReaderProp<T>(name: String, private val prop: KProperty1<T, Any?>) : PropertyReader<T>(name) {
        override val returnType: KType
            get() = prop.returnType

        override fun getValue(pojo: T): Any? {
            return prop.get(pojo)
        }
    }

    // for java getter functions (getField())
    internal class PropertyReaderGetter<T>(name: String, private val getter: KFunction<Any?>) : PropertyReader<T>(name) {
        override val returnType: KType
            get() = getter.returnType

        override fun getValue(pojo: T): Any? {
            return getter.call(pojo)
        }
    }

    internal class PropertyReaderCompiled<T>(name: String, private val propReader: PropReader, override val returnType: KType) : PropertyReader<T>(name) {
        override fun getValue(pojo: T): Any? {
            return propReader.readVal(pojo)
        }
    }

    private fun <T> resolvePropertyReader(
        name: String,
        prop: KProperty1<T, Any?>?,
        getter: KFunction<Any?>?, // for java getter functions (getField())
        precompile: Boolean = false,
        sourceClass: KClass<*>,
    ): PropertyReader<T> {
        val propReader = if (precompile) {
            try {
                val p = if (prop != null) {
                    LaHardReflect.createReaderClass(sourceClass.java, prop.name)
                } else if (getter != null) {
                    LaHardReflect.createReaderClass(sourceClass.java, getter.javaMethod)
                } else {
                    throw NullPointerException("One of prop or getter is mandatory")
                }
                if (p == null) {
                    LaMapper.logger.debug("Can't precompile reading, use reflection (prop=$name class=$sourceClass)")
                }
                p
            } catch (e: Throwable) {
                LaMapper.logger.debug("Can't precompile reading, use reflection (prop=$name class=$sourceClass): ${e.message}")
                null
            }
        } else {
            null
        }

        return if (propReader != null) {
            return PropertyReaderCompiled(name, propReader, prop?.returnType ?: getter?.returnType!!)
        } else if (prop != null) {
            return PropertyReaderProp(name, prop)
        } else if (getter != null) {
            PropertyReaderGetter(name, getter)
        } else {
            throw NullPointerException("One of prop or getter is mandatory")
        }
    }

    internal abstract class PropertyWriter<T>(
        val name: String,
    ) {
        abstract val returnType: KType
        val klass: KClass<*>
            get() = returnType.classifier as KClass<*>

        abstract fun setValue(pojo: T, value: Any?)
    }

    internal class PropertyWriterProp<T>(name: String, private val prop: KMutableProperty1<T, Any?>) : PropertyWriter<T>(name) {
        override val returnType: KType = prop.returnType

        override fun setValue(pojo: T, value: Any?) {
            return prop.set(pojo, value)
        }
    }

    // for java setter functions (setField(x))
    internal class PropertyWriterSetter<T>(name: String, private val setter: KFunction<*>) : PropertyWriter<T>(name) {
        override val returnType: KType = setter.parameters.last().type

        override fun setValue(pojo: T, value: Any?) {
            setter.call(pojo, value)
        }
    }

    internal class PropertyWriterCompiled<T>(name: String, private val propWriter: PropWriter, override val returnType: KType) : PropertyWriter<T>(name) {
        override fun setValue(pojo: T, value: Any?) {
            propWriter.writeVal(pojo, value)
        }
    }

    private fun <T> resolvePropertyWriter(
        name: String,
        prop: KMutableProperty1<T, Any?>?,
        setter: KFunction<*>?,
        precompile: Boolean = false,
        targetClass: KClass<*>,
    ): PropertyWriter<T> {
        val propWriter = if (precompile) {
            try {
                val p = if (prop != null) {
                    LaHardReflect.createWriterClass(targetClass.java, prop.name)
                } else if (setter != null) {
                    LaHardReflect.createWriterClass(targetClass.java, setter.javaMethod)
                } else {
                    throw NullPointerException("One of prop or getter is mandatory")
                }
                if (p == null) {
                    LaMapper.logger.debug("Can't precompile writing, use reflection (prop=$name class=$targetClass)")
                }
                p
            } catch (e: Throwable) {
                LaMapper.logger.debug("Can't precompile writing, use reflection (prop=$name class=$targetClass): ${e.message}")
                null
            }
        } else {
            null
        }

        return if (propWriter != null) {
            return PropertyWriterCompiled(name, propWriter, prop?.returnType ?: setter?.parameters?.last()?.type!!)
        } else if (prop != null) {
            return PropertyWriterProp(name, prop)
        } else if (setter != null) {
            PropertyWriterSetter(name, setter)
        } else {
            throw NullPointerException("One of prop or setter is mandatory")
        }
    }

    internal class PropAutoMapper<Fr, To>(
        val sourceProp: PropertyReader<Fr>,
        val targetProp: PropertyWriter<To>,
        val convFn: ConvFn?,
    )

    internal class PropManualMapper<Fr : Any, To : Any>(
        val targetProp: PropertyWriter<To>,
        val manualMapper: ManualMapper<Fr>,
    )

    private fun init() {
        val sourcePropsByName: Map<String, PropertyReader<Fr>> = getSourceMemberProps(sourceType).associateBy { it.name }
        val targetFieldMap: Map<String, PropertyWriter<To>> = getTargetMemberProps(targetType)
            .associateBy { it.name }
        val targetArgsMandatory: List<KParameter> = targetConstructor?.parameters
            ?.filter { it.name in manualMappers || it.name in sourcePropsByName || !it.isOptional }
            ?: listOf() // may be null for Java classes - then will use no-arg constructor

        propAutoMappers = initPropAutoMappers(sourceType, targetType, manualMappers.keys + targetArgsMandatory.mapNotNull { it.name })

        propManualMappers = manualMappers.filter { arg -> targetConstructor?.parameters?.none { it.name == arg.key } ?: true }
            .filter { it.key in targetFieldMap }
            .map { PropManualMapper(targetFieldMap[it.key]!!, it.value) }

        initManualMapperDataConverters(targetFieldMap)

        paramMappers = targetArgsMandatory.mapNotNull { param ->
            val manMapper = manualMappers[param.name]
            if (manMapper != null) {
                ParamMapper(param, manMapper, null, manMapper.convFn)
            } else {
                val prop = sourcePropsByName[param.name]
                if (prop == null && !param.isOptional)
                    throw IllegalArgumentException("Parameter '${param.name}' is missing")
                if (prop == null) {
                    null
                } else {
                    val sourceKlass: KClass<*> = prop.klass
                    val targetKlass: KClass<*> = param.type.classifier as KClass<*>
                    val convFn = dataConverters.getConverter(sourceKlass, targetKlass) { "sourceField=${prop.name} param=${param.name}" }
                    ParamMapper(param, null, prop, convFn)
                }
            }
        }
    }

    private fun initManualMapperDataConverters(targetFieldMap: Map<String, PropertyWriter<To>>) {
        val paramMap = targetConstructor?.parameters?.associateBy { it.name } ?: mapOf()

        for ((name, manMapper) in manualMappers) {
            manMapper.sourceType ?: continue
            if (manMapper.sourceType.classifier !is KClass<*>)
                continue

            val targetType = paramMap[name]?.type   // constructor param
                ?: targetFieldMap[name]?.returnType // property
                ?: continue
            if (targetType.classifier !is KClass<*>)
                continue

            val sourceKlass: KClass<*> = manMapper.sourceType.classifier as KClass<*>
            val targetKlass: KClass<*> = targetType.classifier as KClass<*>
            manMapper.targetType = targetType
            manMapper.convFn = dataConverters.getConverter(sourceKlass, targetKlass) { "field=$name" }
        }
    }

    private fun getGetterByName(sourceClass: KClass<*>, fieldName: String, type: KType): KFunction<*>? {
        if (fieldName.isEmpty())
            return null
        val fnName = "get" + fieldName[0].uppercaseChar() + fieldName.substring(1)
        return sourceClass.declaredFunctions.find { f -> f.name == fnName && f.returnType == type }
    }

    private fun getSetterByName(sourceClass: KClass<*>, fieldName: String, type: KType): KFunction<*>? {
        if (fieldName.isEmpty())
            return null
        val fnName = "set" + fieldName[0].uppercaseChar() + fieldName.substring(1)
        return sourceClass.declaredFunctions
            .find { it.name == fnName && it.parameters.size == 2 && it.parameters.last().type == type }
    }

    private fun getSourceMemberProps(sourceType: KClass<Fr>): List<PropertyReader<Fr>> {
        return sourceType.memberProperties
            .mapNotNull {
                if (it.visibility in config.visibilities)
                    resolvePropertyReader(it.name, it, null, config.partiallyCompile, sourceType)
                else {
                    val getter: KFunction<*>? = getGetterByName(sourceType, it.name, it.returnType) // case for java getters
                    if (getter != null && getter.visibility in config.visibilities)
                        resolvePropertyReader(it.name, null, getter, config.partiallyCompile, sourceType)
                    else
                        null
                }
            }
    }

    private fun getTargetMemberProps(targetType: KClass<To>): List<PropertyWriter<To>> {
        return targetType.memberProperties
            .mapNotNull {
                @Suppress("UNCHECKED_CAST")
                if (it.visibility in config.visibilities && it is KMutableProperty1)
                    resolvePropertyWriter(it.name, it as KMutableProperty1<To, Any?>, null, config.partiallyCompile, targetType)
                else { // try check case for java setters
                    val setter: KFunction<*>? = getSetterByName(targetType, it.name, it.returnType)
                    if (setter != null && setter.visibility in config.visibilities)
                        resolvePropertyWriter(it.name, null, setter, config.partiallyCompile, targetType)
                    else
                        null
                }
            }
    }

    private fun initPropAutoMappers(
        sourceClass: KClass<Fr>,
        targetClass: KClass<To>,
        skip: Set<String>,
    ): List<PropAutoMapper<Fr, To>> {
        val propsFr: Map<String, PropertyReader<Fr>> = getSourceMemberProps(sourceClass)
            .filterNot { it.name in skip }
            .associateBy { it.name }

        return getTargetMemberProps(targetClass)
            .filter { it.name in propsFr }
            .mapNotNull {
                val pfr = propsFr[it.name]!!
                val convFn = dataConverters.getConverter(pfr.klass, it.klass)
                if (convFn == null)
                    null
                else PropAutoMapper(
                    sourceProp = pfr,
                    targetProp = it,
                    convFn = convFn,
                )
            }
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

internal fun ConvFn?.convertVal(v: Any?): Any? = if (v == null) null else (if (this == null) v else this.convert(v))
