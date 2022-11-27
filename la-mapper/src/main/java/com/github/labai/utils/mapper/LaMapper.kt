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
import com.github.labai.utils.convert.LaConvUtils.ClassPairMap
import com.github.labai.utils.convert.LaConverterRegistry
import java.util.concurrent.atomic.AtomicBoolean
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
 * usage e.g.
 *  val result: ResultDto = LaMapper.copyFrom(from) {
 *      ResultDto:address from { "${it.address}, Vilnius" }
 *  }
 */
class LaMapper(
    private val laConverterRegistry: IConverterResolver
) {
    private val cache: ClassPairMap<AutoMapper<*, *>> = ClassPairMap()

    private val autoConvertNullForPrimitive = true // do auto-convert null to 0 for non-nullable Numbers and Boolean
    private val autoConvertNullToString = true // do auto-convert null to "" for non-nullable Strings

    companion object {
        val global = LaMapper(LaConverterRegistry.global)

        inline fun <reified Fr : Any, reified To : Any> copyFrom(
            from: Fr,
            noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null
        ): To {
            return global.copyFrom(from, Fr::class, To::class, mapping)
        }

        inline fun <reified Fr : Any, reified To : Any> autoMapper(
            noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null
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
        noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null
    ): To {
        return copyFrom(from, Fr::class, To::class, mapping)
    }

    //
    // create mapper for Fr->To classes mapping
    //
    inline fun <reified Fr : Any, reified To : Any> autoMapper(
        noinline mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null
    ): AutoMapper<Fr, To> {
        return autoMapper(Fr::class, To::class, mapping)
    }

    fun <Fr : Any, To : Any> copyFrom(
        from: Fr,
        sourceType: KClass<Fr>,
        targetType: KClass<To>,
        mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null
    ): To {
        @Suppress("UNCHECKED_CAST")
        val mapper = cache.getOrPut(from::class.java, targetType.java) {
            autoMapper(sourceType, targetType, mapping)
        } as AutoMapper<Fr, To>
        return mapper.transform(from)
    }

    fun <Fr : Any, To : Any> autoMapper(
        sourceType: KClass<Fr>,
        targetType: KClass<To>,
        mapping: (MapperBuilder<Fr, To>.() -> Unit)? = null
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
                val valTo = fmap.sourceProp.get(from)?.let { fmap.convFn?.convert(it) } ?: convertNull(fmap.targetProp.returnType)
                fmap.targetProp.set(target, valTo)
            }

            // ordinary (non-constructor) fields, manually mapped
            for ((toProp, mapFn) in propsMapper) {
                val valTo = mapFn.invoke(from)
                val valConv = if (valTo == null) convertNull(toProp.returnType) else convert(valTo, toProp)
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
            value ?: return convertNull(param.type)
            if (param.type == value::class)
                return value
            return convert(value, param)
        }

        private fun convert(value: Any, targetParam: KParameter): Any? {
            val name = targetParam.name ?: return null
            val conv = afterMapConverters.getOrPut(name) {
                if (targetParam.type.classifier is KClass<*>) {
                    val sourceClass: Class<*> = value::class.java
                    val targetClass: Class<*> = (targetParam.type.classifier as KClass<*>).java
                    getConverter(sourceClass, targetClass)
                        ?: throw java.lang.IllegalArgumentException("Mapping not found for constructor argument '$name' ($sourceClass to $targetClass)")
                } else {
                    throw java.lang.IllegalArgumentException("Invalid class constructor argument for field '$name'")
                }
            }
            return conv.convert(value)
        }

        private fun convert(value: Any, targetProp: KMutableProperty1<To, in Any?>): Any? {
            val name = targetProp.name
            val conv = afterMapConverters.getOrPut(name) {
                val sourceClass: Class<*> = value::class.java
                val targetClass: Class<*> = (targetProp.returnType.classifier as KClass<*>).java
                getConverter(sourceClass, targetClass)
                    ?: throw java.lang.IllegalArgumentException("Mapping not found for class field '$name' ($sourceClass to $targetClass)")
            }
            return conv.convert(value)
        }

        private fun initPropConverters(
            sourceClass: KClass<Fr>,
            targetClass: KClass<To>,
            skip: Set<String>
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
                    val convFn = getConverter(propsFr[it.name] as KProperty1<Fr, *>, it)
                    if (convFn == null)
                        null
                    else PropMap(
                        sourceProp = propsFr[it.name]!!,
                        targetProp = it,
                        convFn = convFn
                    )
                }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <Fr, To> getConverter(sourceType: Class<Fr>, targetType: Class<To>): ConvFn? {
            return laConverterRegistry.getConverter(sourceType, targetType) as ConvFn?
        }

        @Suppress("UNCHECKED_CAST")
        private fun <Fr, To> getConverter(sourceType: KProperty1<Fr, *>, targetType: KProperty1<To, *>): ConvFn? {
            val sourceClass: Class<*> = (sourceType.returnType.classifier as KClass<*>).java
            val targetClass: Class<*> = (targetType.returnType.classifier as KClass<*>).java
            return laConverterRegistry.getConverter(sourceClass, targetClass) as ConvFn?
        }

        private fun convertNull(klass: KClass<*>): Any? {
            if (klass.java == String::class.java)
                return if (autoConvertNullToString) "" else null
            if (!autoConvertNullForPrimitive)
                return null
            when (klass) {
                Boolean::class -> return false
                Char::class -> return '\u0000'
                Byte::class -> return 0
                Short::class -> return 0
                Int::class -> return 0
                Float::class -> return 0.0f
                Long::class -> return 0L
                Double::class -> return 0.0
            }
            return null
        }

        private fun convertNull(targetType: KType): Any? {
            if (targetType.isMarkedNullable)
                return null
            if (targetType.classifier !is KClass<*>)
                return null
            val klass = targetType.classifier as KClass<*>
            return convertNull(klass)
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

private typealias ConvFn = ITypeConverter<in Any, out Any?>
