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
import com.github.labai.utils.convert.LaConvertException
import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.LaMapper.ConverterConfig
import org.jetbrains.annotations.TestOnly
import java.math.BigInteger
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
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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
    @get:TestOnly
    internal val cache: ClassTrioMap<AutoMapper<*, *>> = ClassTrioMap()
    private val converterUtils = ConverterUtils(laConverterRegistry, ConverterConfig())

    // ...not configurable yet
    internal class ConverterConfig {
        internal val autoConvertNullForPrimitive = true // do auto-convert null to 0 for non-nullable Numbers and Boolean
        internal val autoConvertNullToString = true // do auto-convert null to "" for non-nullable Strings
        internal val autoConvertValueClass = true // convert value class to/from primitives
        internal val autoCovertValueValue = true // convert between different value classes - even if we can, it may violate the idea of value classes
    }

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

    private class PropMap<Fr, To>(
        val sourceProp: KProperty1<Fr, Any?>,
        val targetProp: KMutableProperty1<To, in Any?>,
        val convFn: ConvFn?,
    )

    private inner class AutoMapperImpl<Fr : Any, To : Any>(
        private val sourceType: KClass<Fr>,
        private val targetType: KClass<To>,
        private val argMapConf: Map<String, (Fr) -> Any?> = mapOf(),
    ) : AutoMapper<Fr, To> {

        private var targetConstructor: KFunction<To>? = null
        private lateinit var targetArgsMandatory: List<KParameter>
        private lateinit var propsConverters: List<PropMap<Fr, To>>
        private lateinit var propsMapper: Map<KMutableProperty1<To, in Any?>, (Fr) -> Any?>
        private lateinit var sourcePropsByName: Map<String, KProperty1<Fr, Any?>>
        private var allArgsNullsTemplate: Array<Any?>? = null
        private val isInitialized = AtomicBoolean(false)
        private val afterMapConverters: MutableMap<String, ConvFn> = mutableMapOf()

        private fun init() {
            if (isInitialized.compareAndSet(false, true)) {
                sourcePropsByName = sourceType.memberProperties.associateBy { it.name }
                val targetFieldMap = targetType.memberProperties
                    .filterIsInstance<KMutableProperty1<To, in Any?>>()
                    .onEach { it.isAccessible = true }
                    .associateBy { it.name }
                targetConstructor = targetType.primaryConstructor
                targetArgsMandatory = targetConstructor?.parameters
                    ?.filter { it.name in argMapConf || it.name in sourcePropsByName || !it.isOptional }
                    ?: listOf() // may be null for Java classes - then will use no-arg constructor
                propsConverters = initPropConverters(sourceType, targetType, argMapConf.keys + targetArgsMandatory.mapNotNull { it.name })
                propsMapper = argMapConf.filter { arg -> targetConstructor!!.parameters.none { it.name == arg.key } }
                    .filter { it.key in targetFieldMap }
                    .map { targetFieldMap[it.key]!! to it.value }
                    .toMap()
                if (targetConstructor != null && targetArgsMandatory.size == targetConstructor!!.parameters.size) {
                    allArgsNullsTemplate = arrayOfNulls(targetConstructor!!.parameters.size)
                }
            }
        }

        override fun transform(from: Fr): To {
            init()

            val target: To
            if (targetConstructor == null) {
                target = targetType.createInstance()
            } else if (allArgsNullsTemplate != null) {
                // args as array are slightly faster
                val paramArr = allArgsNullsTemplate!!.clone()
                var i = 0
                for (param in targetConstructor!!.parameters) {
                    paramArr[i++] = argFor(param, from)
                }
                target = targetConstructor!!.call(*paramArr)
            } else {
                // args as map
                val params = targetArgsMandatory.associateWith { param ->
                    argFor(param, from)
                }
                target = targetConstructor!!.callBy(params)
            }

            // ordinary (non-constructor) fields, auto mapped
            for (fmap in propsConverters) {
                val valTo = fmap.sourceProp.get(from)?.let { fmap.convFn?.convert(it) } ?: converterUtils.convertNull(fmap.targetProp.returnType)
                fmap.targetProp.set(target, valTo)
            }

            // ordinary (non-constructor) fields, manually mapped
            for ((toProp, mapFn) in propsMapper) {
                val valTo = mapFn.invoke(from)
                val valConv = if (valTo == null) converterUtils.convertNull(toProp.returnType) else convertProp(valTo, toProp)
                toProp.set(target, valConv)
            }
            return target
        }

        private fun argFor(param: KParameter, from: Fr): Any? {
            val name = param.name ?: return null
            val mapFn = argMapConf[name]
            val value = if (mapFn != null) {
                mapFn.invoke(from)
            } else {
                val prop = sourcePropsByName[name]
                if (prop == null && !param.isOptional)
                    throw IllegalArgumentException("Parameter '$name' is missing")
                prop?.get(from)
            }
            value ?: return converterUtils.convertNull(param.type)
            if (param.type == value::class)
                return value
            return convertArg(value, param)
        }

        private fun convertArg(value: Any, targetParam: KParameter): Any? {
            val name = targetParam.name ?: return null
            val conv = afterMapConverters.getOrPut(name) {
                if (targetParam.type.classifier is KClass<*>) {
                    val sourceKlass: KClass<*> = value::class
                    val targetKlass: KClass<*> = (targetParam.type.classifier as KClass<*>)
                    converterUtils.getConverter(sourceKlass, targetKlass)
                        ?: throw java.lang.IllegalArgumentException("Mapping not found for constructor argument '$name' ($sourceKlass to $targetKlass)")
                } else {
                    throw java.lang.IllegalArgumentException("Invalid class constructor argument for field '$name'")
                }
            }
            return conv.convert(value)
        }

        private fun convertProp(value: Any, targetProp: KMutableProperty1<To, in Any?>): Any? {
            val name = targetProp.name
            val conv = afterMapConverters.getOrPut(name) {
                val sourceKlass: KClass<*> = value::class
                val targetKlass: KClass<*> = (targetProp.returnType.classifier as KClass<*>)
                converterUtils.getConverter(sourceKlass, targetKlass)
                    ?: throw java.lang.IllegalArgumentException("Mapping not found for class field '$name' ($sourceKlass to $targetKlass)")
            }
            return conv.convert(value)
        }

        private fun initPropConverters(
            sourceClass: KClass<Fr>,
            targetClass: KClass<To>,
            skip: Set<String>,
        ): List<PropMap<Fr, To>> {
            val propsFr: Map<String, KProperty1<Fr, *>> = sourceClass.memberProperties
                .filterNot { it.name in skip }
                .onEach { it.isAccessible = true }
                .associateBy { it.name }

            return targetClass.memberProperties
                .filterIsInstance<KMutableProperty1<To, in Any?>>()
                .filter { it.name in propsFr }
                .onEach { it.isAccessible = true }
                .mapNotNull {
                    val convFn = converterUtils.getConverter(propsFr[it.name] as KProperty1<Fr, *>, it)
                    if (convFn == null)
                        null
                    else PropMap(
                        sourceProp = propsFr[it.name]!!,
                        targetProp = it,
                        convFn = convFn,
                    )
                }
        }
    }

    open class MapperBuilder<Fr, To> {
        val map: MutableMap<String, (Fr) -> Any?> = mutableMapOf()

        // e.g.
        //  To::address from From:address
        infix fun <V1, V2> KProperty1<To, V1>.from(sourceRef: KProperty1<Fr, V2>) {
            map[this.name] = sourceRef::get
        }

        // e.g.
        //  To::address from { it.address + ", Vilnius" }
        infix fun <V> KProperty1<To, V>.from(sourceFn: (Fr) -> V) {
            map[this.name] = sourceFn
        }

        // e.g.
        //  From::address mapTo To::address
        infix fun <V1, V2> KProperty1<Fr, V1>.mapTo(targetRef: KProperty1<To, V2>) {
            map[targetRef.name] = this::get
        }
    }
}

interface AutoMapper<Fr : Any, To : Any> {
    fun transform(from: Fr): To
}

internal class ConverterUtils(
    private val laConverterRegistry: IConverterResolver,
    private val converterConfig: ConverterConfig,
) {
    private val unumberConverterResolver = KotlinUNumberConverterResolver(laConverterRegistry)

    fun <Fr, To> getConverter(sourceType: KProperty1<Fr, *>, targetType: KProperty1<To, *>): ConvFn? {
        val sourceKlass: KClass<*> = (sourceType.returnType.classifier as KClass<*>)
        val targetKlass: KClass<*> = (targetType.returnType.classifier as KClass<*>)
        return getConverter(sourceKlass, targetKlass)
    }

    @Suppress("UNCHECKED_CAST")
    fun getConverter(sourceKlass: KClass<*>, targetKlass: KClass<*>): ConvFn? {
        var convFn = getLaConverter(sourceKlass, targetKlass)
        if (convFn != null)
            return convFn
        if (!converterConfig.autoConvertValueClass)
            return null

        // for value classes try more combination (value to/from simple)
        val sourceUnwrapped = getCustomUnwrappedTypeOrNull(sourceKlass)
        if (sourceUnwrapped != null) {
            val fn = getLaConverter(sourceUnwrapped, targetKlass)
            if (fn != null)
                return wrapSourceValueClassConverter(sourceKlass as KClass<Any>, fn)
        }

        val targetUnwrapped = getCustomUnwrappedTypeOrNull(targetKlass)
        if (targetUnwrapped != null && targetUnwrapped != String::class) { // laConverter can convert anything to String. Exclude this case for value class
            val fn = getLaConverter(sourceKlass, targetUnwrapped)
            if (fn != null)
                return wrapTargetValueClassConverter(targetKlass as KClass<Any>, fn)
        }

        if (converterConfig.autoCovertValueValue) {
            if (sourceUnwrapped != null && targetUnwrapped != null) {
                val fn = getLaConverter(sourceUnwrapped, targetUnwrapped)
                if (fn != null)
                    return wrapSourceAndTargetValueClassConverter(sourceKlass as KClass<Any>, targetKlass as KClass<Any>, fn)
            }
        }

        convFn = unumberConverterResolver.getKConverter(sourceKlass, targetKlass)
        if (convFn != null)
            return convFn

        throw LaConvertException("Convert case is not defined (targetType=$sourceKlass, sourceType=$targetKlass)")
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLaConverter(sourceKlass: KClass<*>, targetKlass: KClass<*>): ConvFn? {
        try {
            return laConverterRegistry.getConverter(sourceKlass.java, targetKlass.java) as ConvFn?
        } catch (e: LaConvertException) {
            // continue
        }
        return unumberConverterResolver.getKConverter(sourceKlass, targetKlass)
    }

    private fun wrapSourceValueClassConverter(klass: KClass<Any>, convFn: ConvFn): ConvFn? {
        val mainProp = klass.memberProperties.singleOrNull() ?: return null
        return ITypeConverter { convFn.convert(mainProp.get(it as Any)) }
    }

    private fun wrapTargetValueClassConverter(klass: KClass<Any>, convFn: ConvFn): ConvFn? {
        val mainConstr = klass.primaryConstructor ?: return null
        return ITypeConverter { convFn.convert(it)?.let { res -> mainConstr.call(res) } }
    }

    private fun wrapSourceAndTargetValueClassConverter(srcKlass: KClass<Any>, trgKlass: KClass<Any>, convFn: ConvFn): ConvFn? {
        val mainProp = srcKlass.memberProperties.singleOrNull() ?: return null
        val mainConstr = trgKlass.primaryConstructor ?: return null
        return ITypeConverter { convFn.convert(mainProp.get(it as Any))?.let { res -> mainConstr.call(res) } }
    }

    private fun getCustomUnwrappedTypeOrNull(mainKlass: KClass<*>): KClass<*>? {
        if (!mainKlass.isValue)
            return null
        return mainKlass.primaryConstructor?.parameters?.singleOrNull()?.type?.classifier as KClass<*>?
    }

    private fun convertNull(klass: KClass<*>): Any? {
        if (klass.java == String::class.java)
            return if (converterConfig.autoConvertNullToString) "" else null
        if (!converterConfig.autoConvertNullForPrimitive)
            return null
        when (klass) {
            Boolean::class -> return false
            Char::class -> return '\u0000'
            Byte::class -> return 0
            UByte::class -> return 0
            Short::class -> return 0
            UShort::class -> return 0
            Int::class -> return 0
            UInt::class -> return 0
            Long::class -> return 0L
            ULong::class -> return 0L
            Float::class -> return 0.0f
            Double::class -> return 0.0
        }
        return null
    }

    fun convertNull(targetType: KType): Any? {
        if (targetType.isMarkedNullable)
            return null
        if (targetType.classifier !is KClass<*>)
            return null
        val klass = targetType.classifier as KClass<*>
        return convertNull(klass)
    }
}

// UByte, UShort, UInt, ULong
internal class KotlinUNumberConverterResolver(
    private val laConverterRegistry: IConverterResolver,
) {

    fun <Fr : Any, To : Any> getKConverter(sourceKType: KClass<Fr>, targetKType: KClass<To>): ConvFn? {
        val resultKlass: KClass<out Any>?
        val resultConv: ((Any?) -> Any?)?
        when (targetKType) {
            UByte::class -> {
                resultKlass = Short::class
                resultConv = { it -> (it as Short).toUByte() }
            }
            UShort::class -> {
                resultKlass = Int::class
                resultConv = { (it as Int).toUShort() }
            }
            UInt::class -> {
                resultKlass = Long::class
                resultConv = { (it as Long).toUInt() }
            }
            ULong::class -> {
                resultKlass = BigInteger::class
                resultConv = { (it as BigInteger).toString().toULong() }
            }
            else -> {
                resultKlass = targetKType
                resultConv = { it }
            }
        }

        when (sourceKType) {
            UByte::class -> {
                val convFn = getLaConverter(Short::class, resultKlass)
                if (convFn != null)
                    return ITypeConverter { resultConv(convFn.convert((it as UByte).toShort())) }
            }
            UShort::class -> {
                val convFn = getLaConverter(Int::class, resultKlass)
                if (convFn != null) {
                    return ITypeConverter { resultConv(convFn.convert((it as UShort).toInt())) }
                }
            }
            UInt::class -> {
                val convFn = getLaConverter(Long::class, resultKlass)
                if (convFn != null)
                    return ITypeConverter { resultConv(convFn.convert((it as UInt).toLong())) }
            }
            ULong::class -> {
                val convFn = getLaConverter(BigInteger::class, resultKlass)
                if (convFn != null)
                    return ITypeConverter { resultConv(convFn.convert((it as ULong).toString().toBigInteger())) }
            }
            else -> {
                if (resultKlass != targetKType) {
                    val convFn = getLaConverter(sourceKType, resultKlass)
                    if (convFn != null)
                        return ITypeConverter { resultConv(convFn.convert(it)) }
                }
            }
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLaConverter(sourceKlass: KClass<*>, targetKlass: KClass<*>): ConvFn? {
        return try {
            return laConverterRegistry.getConverter(sourceKlass.java, targetKlass.java) as ConvFn?
        } catch (e: LaConvertException) {
            null
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

private typealias ConvFn = ITypeConverter<in Any, out Any?>
