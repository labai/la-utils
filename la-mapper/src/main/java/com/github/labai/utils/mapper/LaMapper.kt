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
import com.github.labai.utils.mapper.impl.LaMapperImpl
import com.github.labai.utils.mapper.impl.LaMapperImpl.ManualMapping
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
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
 *  val result: ResultDto = LaMapper.copyFrom(from) {
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
        @Suppress("UNCHECKED_CAST")
        val mapper = cache.getOrPut(from::class, targetType, if (mapping == null) null else mapping::class) {
            autoMapper(sourceType, targetType, mapping)
        } as AutoMapper<Fr, To>
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
            laMapperImpl.AutoMapperImpl(sourceType, targetType, builder.map)
        } else {
            laMapperImpl.AutoMapperImpl(sourceType, targetType, emptyMap())
        }
    }

    open class MappingBuilder<Fr, To> {
        internal val map: MutableMap<String, ManualMapping<Fr>> = mutableMapOf()

        // e.g.
        //  To::address from From::address
        infix fun <V1, V2> KProperty1<To, V1>.from(sourceRef: KProperty1<Fr, V2>) {
            map[this.name] = ManualMapping(sourceRef::get, sourceRef.returnType)
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
            map[this.name] = ManualMapping(sourceFn, returnType)
        }

        // e.g.
        //  From::address mapTo To::address
        infix fun <V1, V2> KProperty1<Fr, V1>.mapTo(targetRef: KProperty1<To, V2>) {
            map[targetRef.name] = ManualMapping(this::get, this.returnType)
        }
    }
}

interface AutoMapper<Fr : Any, To : Any> {
    fun transform(from: Fr): To
}
