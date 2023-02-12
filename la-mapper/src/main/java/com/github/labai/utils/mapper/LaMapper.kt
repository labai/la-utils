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
import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.LaMapper.ConverterConfig
import com.github.labai.utils.mapper.LaMapper.ManualMapper
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.KVisibility.INTERNAL
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

/**
 * @author Augustus
 *         created on 2022.11.15
 *
 * https://github.com/labai/la-utils/tree/main/la-mapper
 *
 * usage e.g.
 *  val result: ResultDto = LaMapper.copyFrom(from) {
 *      ResultDto:address from { "${it.address}, Vilnius" }
 *  }
 *
 */
class LaMapper(
    laConverterRegistry: IConverterResolver,
) {

    @TestOnly
    internal constructor(laConverterRegistry: IConverterResolver, config: ConverterConfig) : this(laConverterRegistry) {
        this.config = config
    }

    @get:TestOnly
    internal val cache: ClassTrioMap<AutoMapper<*, *>> = ClassTrioMap()
    internal var config = ConverterConfig()
        private set
    internal val dataConverters = DataConverters(laConverterRegistry, config)
    private val mapperCompiler = MapperCompiler(this)

    // ...configurable only for tests yet
    internal data class ConverterConfig (
        internal val autoConvertNullForPrimitive: Boolean = true, // do auto-convert null to 0 for non-nullable Numbers and Boolean
        internal val autoConvertNullToString: Boolean = true, // do auto-convert null to "" for non-nullable Strings
        internal val autoConvertValueClass: Boolean = true, // convert value class to/from primitives
        internal val autoConvertValueValue: Boolean = true, // convert between different value classes - even if we can, it may violate the idea of value classes
        internal val tryCompile: Boolean = false, // try to compile
        internal val startCompileAfterIterations: Int = 1000, // start to compile after n iterations
        internal val visibilities: Set<KVisibility> = setOf(PUBLIC, INTERNAL),
    )

    companion object {
        val global = LaMapper(LaConverterRegistry.global)

        inline fun <reified Fr : Any, reified To : Any> copyFrom(
            from: Fr,
            noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null,
        ): To {
            return global.copyFrom(from, Fr::class, To::class, mapping)
        }

        inline fun <reified Fr : Any, reified To : Any> autoMapper(
            noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null,
        ): AutoMapper<Fr, To> {
            return global.autoMapper(Fr::class, To::class, mapping)
        }
    }

    //
    // create <To> and copy fields from <Fr> to <To>
    //
    // cache mapper - if mapper already exist, will use it
    // (you can not have 2 different mappers from Fr->To with this function)
    //
    inline fun <reified Fr : Any, reified To : Any> copyFrom(
        from: Fr,
        noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null,
    ): To {
        return copyFrom(from, Fr::class, To::class, mapping)
    }

    //
    // create mapper for Fr->To classes mapping
    //
    inline fun <reified Fr : Any, reified To : Any> autoMapper(
        noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null,
    ): AutoMapper<Fr, To> {
        return autoMapper(Fr::class, To::class, mapping)
    }

    fun <Fr : Any, To : Any> copyFrom(
        from: Fr,
        sourceType: KClass<Fr>,
        targetType: KClass<To>,
        mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null,
    ): To {
        @Suppress("UNCHECKED_CAST")
        val mapper = cache.getOrPut(from::class, targetType, if (mapping == null) null else mapping::class) {
            autoMapper(sourceType, targetType, mapping)
        } as AutoMapper<Fr, To>
        return mapper.transform(from)
    }

    fun <Fr : Any, To : Any> autoMapper(
        sourceType: KClass<Fr>,
        targetType: KClass<To>,
        mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null,
    ): AutoMapper<Fr, To> {
        return if (mapping != null) {
            val builder = MapperBuilder<Fr, To>()
            builder.mapping()
            AutoMapperImpl(sourceType, targetType, builder.map)
        } else {
            AutoMapperImpl(sourceType, targetType, emptyMap())
        }
    }

    internal inner class AutoMapperImpl<Fr : Any, To : Any>(
        internal val sourceType: KClass<Fr>,
        internal val targetType: KClass<To>,
        private var manualMappers: Map<String, ManualMapper<Fr>> = mapOf(),
    ) : AutoMapper<Fr, To> {

        internal lateinit var struct: MappedStruct<Fr, To>
        private var allArgsNullsTemplate: Array<Any?>? = null
        private val isInitialized = AtomicBoolean(false)
        private var needToCompile = config.tryCompile
        private var counter = 0
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
                    CompilerQueue.addTask(mapperCompiler, this.struct) { compiled ->
                        if (compiled != null) {
                            compiledMapper = compiled
                        }
                    }
                    needToCompile = false
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

    open class MapperBuilder<Fr, To> {
        internal val map: MutableMap<String, ManualMapper<Fr>> = mutableMapOf()

        // e.g.
        //  To::address from From:address
        infix fun <V1, V2> KProperty1<To, V1>.from(sourceRef: KProperty1<Fr, V2>) {
            map[this.name] = ManualMapper(sourceRef::get, sourceRef.returnType)
        }

        // e.g.
        //  To::address from { it.address + ", Vilnius" }
        @OptIn(ExperimentalReflectionOnLambdas::class)
        infix fun <V> KProperty1<To, V>.from(sourceFn: (Fr) -> V) {
            val returnType = try {
                val tp = sourceFn.reflect()?.returnType
                if (tp == null || tp.classifier == Any::class) {
                    null
                } else {
                    tp
                }
            } catch (e: Error) {
                null
            }
            map[this.name] = ManualMapper(sourceFn, returnType)
        }

        // e.g.
        //  From::address mapTo To::address
        infix fun <V1, V2> KProperty1<Fr, V1>.mapTo(targetRef: KProperty1<To, V2>) {
            map[targetRef.name] = ManualMapper(this::get, this.returnType)
        }
    }
}

interface AutoMapper<Fr : Any, To : Any> {
    fun transform(from: Fr): To
}

internal class MappedStruct<Fr : Any, To : Any>(
    internal val sourceType: KClass<Fr>,
    internal val targetType: KClass<To>,
    private val manualMappers: Map<String, ManualMapper<Fr>> = mapOf(),
    private val dataConverters: DataConverters,
    private val config: ConverterConfig,
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
        val sourceProp: PropOrGetter<Fr>?,
        val convFn: ConvFn?,
    ) {
        private val paramKlass = param.type.classifier as KClass<*>
        internal fun mapParam(from: Fr): Any? {
            val value = if (manualMapper != null) {
                manualMapper.mapper.invoke(from)
            } else if (sourceProp != null) {
                sourceProp.getValue(from)
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


    internal class PropOrGetter<T>(
        val name: String,
        val prop: KProperty1<T, Any?>?,
        val getter: KFunction<Any?>?, // for java getter functions (getField())
    ) {
        val returnType
            get(): KType = prop?.returnType ?: getter?.returnType!!
        val klass = returnType.classifier as KClass<*>

        fun getValue(pojo: T): Any? {
            return if (prop != null) {
                prop.get(pojo)
            } else if (getter != null) {
                getter.call(pojo)
            } else {
                throw NullPointerException("One of prop or getter is mandatory")
            }
        }

    }

    internal class PropOrSetter<T>(
        val name: String,
        val prop: KMutableProperty1<T, Any?>?,
        val setter: KFunction<*>?,
    ) {
        val returnType
            get(): KType = prop?.returnType ?: setter?.parameters?.last()?.type!!
        val klass = returnType.classifier as KClass<*>

        fun setValue(pojo: T, value: Any?) {
            if (prop != null) {
                prop.set(pojo, value)
            } else if (setter != null) {
                setter.call(pojo, value)
            } else {
                throw NullPointerException("One of prop or getter is mandatory")
            }
        }
    }

    internal class PropAutoMapper<Fr, To>(
        val sourceProp: PropOrGetter<Fr>,
        val targetProp: PropOrSetter<To>,
        val convFn: ConvFn?,
    )
    internal class PropManualMapper<Fr : Any, To : Any>(
        val targetProp: PropOrSetter<To>,
        val manualMapper: ManualMapper<Fr>,
    )


    private fun init() {
        val sourcePropsByName: Map<String, PropOrGetter<Fr>> = getSourceMemberProps(sourceType).associateBy { it.name }
        val targetFieldMap: Map<String, PropOrSetter<To>> = getTargetMemberProps(targetType)
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

    private fun initManualMapperDataConverters(targetFieldMap: Map<String, PropOrSetter<To>>) {
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
        return sourceClass.declaredFunctions.find { f -> f.name == fnName && f.returnType == type}
    }

    private fun getSetterByName(sourceClass: KClass<*>, fieldName: String, type: KType): KFunction<*>? {
        if (fieldName.isEmpty())
            return null
        val fnName = "set" + fieldName[0].uppercaseChar() + fieldName.substring(1)
        return sourceClass.declaredFunctions
            .find { it.name == fnName && it.parameters.size == 2 && it.parameters.last().type == type}
    }

    private fun getSourceMemberProps(sourceType: KClass<Fr>): List<PropOrGetter<Fr>> {
        return sourceType.memberProperties
            .mapNotNull {
                if (it.visibility in config.visibilities)
                    PropOrGetter(it.name, it, null)
                else {
                    val getter: KFunction<*>? = getGetterByName(sourceType, it.name, it.returnType) // case for java getters
                    if (getter != null && getter.visibility in config.visibilities)
                        PropOrGetter(it.name, null, getter)
                    else
                        null
                }
            }
    }


    private fun getTargetMemberProps(targetType: KClass<To>): List<PropOrSetter<To>> {
        return targetType.memberProperties
            .mapNotNull {
                @Suppress("UNCHECKED_CAST")
                if (it.visibility in config.visibilities && it is KMutableProperty1)
                    PropOrSetter(it.name, it as KMutableProperty1<To, Any?>, null)
                else { // try check case for java setters
                    val setter: KFunction<*>? = getSetterByName(targetType, it.name, it.returnType)
                    if (setter != null && setter.visibility in config.visibilities)
                        PropOrSetter(it.name, null, setter)
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
        val propsFr: Map<String, PropOrGetter<Fr>> = getSourceMemberProps(sourceClass)
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

// data type converter (from la-converter)
internal typealias ConvFn = ITypeConverter<in Any, out Any?>

// manual mapping lambda
internal typealias ManualFn<Fr> = (Fr) -> Any?

internal fun ConvFn?.convertVal(v: Any?): Any? = if (v == null) null else (if (this == null) v else this.convert(v))
