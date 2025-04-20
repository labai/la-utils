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
import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.impl.ClassTrioMap
import com.github.labai.utils.mapper.impl.ClosureUtils
import com.github.labai.utils.mapper.impl.LaMapperImpl
import com.github.labai.utils.mapper.impl.LaMapperImpl.AutoMapperImpl
import com.github.labai.utils.mapper.impl.LaMapperImpl.IMappingBuilderItem
import com.github.labai.utils.mapper.impl.LaMapperImpl.LambdaMapping
import com.github.labai.utils.mapper.impl.LaMapperImpl.PropMapping
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.function.Function as JavaFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.KVisibility.INTERNAL
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

/**
 * @author Augustus
 *         created on 2022.11.15
 *
 * https://github.com/labai/la-utils/tree/main/la-mapper
 *
 * usage e.g.
 *  val result: ResultDto: PersonDto = LaMapper.copyFrom(from) {
 *      ResultDto::personId from Source::id
 *      ResultDto::address from { "${it.address}, Vilnius" }
 *  }
 *
 */
class LaMapper(
    laConverterRegistry: IConverterResolver,
    config: ILaMapperConfig = LaMapperConfig(),
) {

    internal val cache: ClassTrioMap<AutoMapper<*, *>> = ClassTrioMap()
    private val laMapperImpl = LaMapperImpl(laConverterRegistry, config)

    interface ILaMapperConfig

    data class LaMapperConfig(
        internal val autoConvertNullForPrimitive: Boolean = true, // do auto-convert null to 0 for non-nullable Numbers and Boolean
        internal val autoConvertNullToString: Boolean = true, // do auto-convert null to "" for non-nullable Strings
        internal val autoConvertValueClass: Boolean = true, // convert value class to/from primitives
        internal val autoConvertValueValue: Boolean = true, // convert between different value classes - even if we can, it may violate idea of value classes
        internal val startCompileAfterIterations: Int = 1000, // start to compile after n iterations
        internal val visibilities: Set<KVisibility> = setOf(PUBLIC, INTERNAL),
        internal val disableCompile: Boolean = false, // disable compile to jvm
        internal val disableSyntheticConstructorCall: Boolean = false, // disable direct kotlin synthetic constructor usage for optional parameters
        internal val disableFullCompile: Boolean = false, // disable full compile (used for tests)
        internal val failOnOptimizationError: Boolean = false, // in case of optimization failure throw an error and don't try to use reflection
        internal val disableClosureLambdas: Boolean = false // disable closure lambdas (which access outer scope context). Mapping with closures is slower
    ) : ILaMapperConfig

    companion object {
        internal val logger = LoggerFactory.getLogger(LaMapper::class.java)
        val global = LaMapper(LaConverterRegistry.global)

        inline fun <reified Fr : Any, reified To : Any> copyFrom(
            from: Fr,
            noinline mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
        ): To {
            return global.copyFrom(from, Fr::class, To::class, mapping)
        }

        inline fun <reified Fr : Any, reified To : Any> autoMapper(
            noinline mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
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
        noinline mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
    ): To {
        return copyFrom(from, Fr::class, To::class, mapping)
    }

    //
    // create mapper for Fr->To classes mapping
    //
    inline fun <reified Fr : Any, reified To : Any> autoMapper(
        noinline mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
    ): AutoMapper<Fr, To> {
        return autoMapper(Fr::class, To::class, mapping)
    }

    fun <Fr : Any, To : Any> copyFrom(
        from: Fr,
        sourceType: KClass<Fr>,
        targetType: KClass<To>,
        mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
    ): To {
        var isCached = true
        @Suppress("UNCHECKED_CAST")
        val mapper = cache.getOrPut(sourceType, targetType, if (mapping == null) null else mapping::class) {
            isCached = false
            autoMapper(sourceType, targetType, mapping)
        } as AutoMapperImpl<Fr, To>

        if (isCached && mapper.hasClosure()) { // ignore cache if there are closures
            val builder = MappingBuilder<Fr, To>()
            if (mapping != null) {
                builder.mapping()
                builder.map
            }
            val mapper2 = ClosureUtils.withOverrides(mapper, builder.map, laMapperImpl.serviceContext)
            return mapper2.transform(from)
        }

        return mapper.transform(from)
    }

    fun <Fr : Any, To : Any> autoMapper(
        sourceType: KClass<Fr>,
        targetType: KClass<To>,
        mapping: (MappingBuilder<Fr, To>.() -> Unit)? = null,
    ): AutoMapper<Fr, To> {
        return if (mapping != null) {
            val builder = MappingBuilder<Fr, To>()
            builder.mapping()
            if (builder.hasClosure && laMapperImpl.serviceContext.config.disableClosureLambdas)
                throw IllegalArgumentException("Mapping lambda contains closures (access objects out of lambda) but closures are disabled in LaMapper config")
            laMapperImpl.AutoMapperImpl(sourceType, targetType, builder.map)
        } else {
            laMapperImpl.AutoMapperImpl(sourceType, targetType, emptyMap())
        }
    }

    @JvmName("copyFromJ")
    internal fun <Fr : Any, To : Any> copyFromJ(
        from: Fr,
        sourceType: Class<Fr>,
        targetType: Class<To>,
        fieldMappers: List<Pair<String, Any>>?,
    ): To {
        val map = getMapFromJavaPairList<Fr>(fieldMappers)

        @Suppress("UNCHECKED_CAST")
        val mapper = cache.getOrPut(sourceType.kotlin, targetType.kotlin, fieldMappers ?: listOf<Pair<String, Any>>()) {
            laMapperImpl.AutoMapperImpl(sourceType.kotlin, targetType.kotlin, map)
        } as AutoMapper<Fr, To>

        return mapper.transform(from)
    }

    @JvmName("autoMapperJ")
    internal fun <Fr : Any, To : Any> autoMapperJ(
        sourceType: Class<Fr>,
        targetType: Class<To>,
        fieldMappers: List<Pair<String, Any>>?,
    ): AutoMapper<Fr, To> {
        val map = getMapFromJavaPairList<Fr>(fieldMappers)
        return laMapperImpl.AutoMapperImpl(sourceType.kotlin, targetType.kotlin, map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Fr : Any> getMapFromJavaPairList(fieldMappers: List<Pair<String, Any>>?): Map<String, IMappingBuilderItem<Fr>> {
        if (fieldMappers.isNullOrEmpty())
            return mapOf()

        val map: MutableMap<String, IMappingBuilderItem<Fr>> = mutableMapOf()
        for ((to, from) in fieldMappers) {
            if (from is JavaFunction<*, *>) {
                val fn = from as JavaFunction<Fr, *>
                map[to] = LambdaMapping({ fn.apply(it) }, null)
            } else {
                error("Invalid 'from' type (${from.javaClass.name}) for target field '$to'")
            }
        }
        return map
    }

    open class MappingBuilder<Fr : Any, To> {
        internal val map: MutableMap<String, IMappingBuilderItem<Fr>> = mutableMapOf()
        internal var hasClosure: Boolean = false
            private set

        /** "from" class instance - dummy object for shorter access to field. DO NOT use it as object, only as reference to field */
        val f: Fr = dummy()

        /** "to" class instance - dummy object for shorter access to field. DO NOT use it as object, only as reference to field */
        val t: To = dummy()

        // ------- <t> from <f> combinations ------------------

        //  Target::address from Source::address
        infix fun <V1, V2> KProperty1<To, V1>.from(sourceRef: KProperty1<Fr, V2>) {
            map[this.name] = PropMapping(sourceRef.name)
        }

        //  Target::address from f::address
        infix fun <V1, V2> KProperty1<To, V1>.from(sourceRef: KCallable<V2>?) {
            val frName = sourceRef?.name ?: throw IllegalStateException("mapper source param is null")
            map[this.name] = PropMapping(frName)
        }

        //  t::address from Source::address
        infix fun <V1, V2> KProperty0<V1>.from(sourceRef: KProperty1<Fr, V2>) {
            map[this.name] = PropMapping(sourceRef.name)
        }

        // t::address from f::address
        infix fun <V1, V2> KProperty0<V1>?.from(sourceRef: KCallable<V2>?) {
            val frName = sourceRef?.name ?: throw IllegalStateException("mapper source param is null")
            val toName = this?.name ?: throw IllegalStateException("mapper target param is null")
            map[toName] = PropMapping(frName)
        }

        //  t::address from Source::address (works for records)
        infix fun <V1, V2> KCallable<V1>.from(sourceRef: KProperty1<Fr, V2>) {
            map[this.name] = PropMapping(sourceRef.name)
        }

        // t::address from f::address (works for records)
        infix fun <V1, V2> KCallable<V1>?.from(sourceRef: KCallable<V2>?) {
            val frName = sourceRef?.name ?: throw IllegalStateException("mapper source param is null")
            val toName = this?.name ?: throw IllegalStateException("mapper target param is null")
            map[toName] = PropMapping(frName)
        }

        //  To::address from { it.address + ", Vilnius" }
        infix fun <V> KProperty1<To, V>.from(sourceFn: (Fr) -> V) {
            addLambdaMapping(this.name, sourceFn)
        }

        //  t::address from { it.address + ", Vilnius" }
        infix fun <V> KProperty0<V>.from(sourceFn: (Fr) -> V) {
            addLambdaMapping(this.name, sourceFn)
        }

        //  t::address from { it.address + ", Vilnius" } (works for records)
        infix fun <V> KCallable<V>.from(sourceFn: (Fr) -> V) {
            addLambdaMapping(this.name, sourceFn)
        }

        // ------- <f> mapTo <t> combinations -----------------

        //  From::address mapTo To::address
        infix fun <V1, V2> KProperty1<Fr, V1>.mapTo(targetRef: KProperty1<To, V2>) {
            map[targetRef.name] = LambdaMapping(this::get, this.returnType)
        }

        //  From::address mapTo t::address
        infix fun <V1, V2> KProperty1<Fr, V1>.mapTo(targetRef: KCallable<V2>) {
            map[targetRef.name] = LambdaMapping(this::get, this.returnType)
        }

        //  f::address mapTo To::address
        infix fun <V1, V2> KProperty0<V1>.mapTo(targetRef: KProperty1<To, V2>) {
            map[targetRef.name] = PropMapping(this.name)
        }

        //  f::address mapTo t::address
        infix fun <V1, V2> KProperty0<V1>.mapTo(targetRef: KCallable<V2>) {
            map[targetRef.name] = PropMapping(this.name)
        }

        //  f::address mapTo t::address (works for records)
        infix fun <V1, V2> KCallable<V1>.mapTo(targetRef: KCallable<V2>) {
            map[targetRef.name] = PropMapping(this.name)
        }

        //  f::address mapTo To::address (works for records)
        infix fun <V1, V2> KCallable<V1>.mapTo(targetRef: KProperty1<To, V2>) {
            map[targetRef.name] = PropMapping(this.name)
        }

        @OptIn(ExperimentalReflectionOnLambdas::class)
        private fun <V> getReturnTypeOfLambda(fn: (Fr) -> V): KType? {
            return try {
                val tp = fn.reflect()?.returnType
                if (tp == null || tp.classifier == Any::class) {
                    null
                } else {
                    tp
                }
            } catch (e: Error) {
                null
            }
        }

        private fun isClosure(lambda: Any): Boolean {
            return lambda.javaClass.declaredFields.any { field -> !field.isSynthetic }
        }

        private fun <T> dummy(): T {
            val o = null as T?
            asNn(o)
            return o
        }

        @OptIn(ExperimentalContracts::class)
        private fun <T> asNn(value: T?) {
            contract { returns() implies (value != null) }
        }

        private fun <V> addLambdaMapping(name: String, sourceFn: (Fr) -> V, retType: KType? = null) {
            val returnType = retType ?: getReturnTypeOfLambda(sourceFn)
            val clos = isClosure(sourceFn)
            if (clos) {
                hasClosure = true
            }
            map[name] = LambdaMapping(sourceFn, returnType, isClosure = clos)
        }
    }
}

interface AutoMapper<Fr : Any, To : Any> {
    fun transform(from: Fr): To
}
