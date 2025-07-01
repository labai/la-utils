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
import com.github.labai.utils.convert.LaConvertException
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * @author Augustus
 *         created on 2023.01.26
 *
 *  Data converting service.
 *
 *  Mostly reuse la-converter, but in addition support few kotlin specific data types
 *  (unsigned numbers, value classes).
 */
internal class DataConverters(
    private val laConverterRegistry: IConverterResolver,
    private val laMapperConfig: LaMapperConfig,
) {
    private val unumberConverterResolver = KotlinUNumberConverterResolver(laConverterRegistry)

    fun <Fr, To> getConverter(sourceType: KProperty1<Fr, *>, targetType: KProperty1<To, *>): ConvFn? {
        val sourceKlass: KClass<*> = (sourceType.returnType.classifier as KClass<*>)
        val targetKlass: KClass<*> = (targetType.returnType.classifier as KClass<*>)
        return getConverter(sourceKlass, targetKlass) { "sourceField=${sourceType.name} targetField=${targetType.name}" }
    }

    fun <To> getConverter(sourceType: KType, targetType: KProperty1<To, *>): ConvFn? {
        val sourceKlass: KClass<*> = (sourceType.classifier as KClass<*>)
        val targetKlass: KClass<*> = (targetType.returnType.classifier as KClass<*>)
        return getConverter(sourceKlass, targetKlass) { "sourceType=$sourceType targetField=${targetType.name}" }
    }

    fun getConverterNn(sourceKlass: KClass<*>, targetKlass: KClass<*>, targetNullable: Boolean, errorDetails: (() -> String)? = null): ConvFn? {
        val c = getConverter(sourceKlass, targetKlass, errorDetails)
        if (!targetNullable || targetKlass.java.isPrimitive)
            return wrapNotNullConverter(c, targetKlass)
        return c
    }

    @Suppress("UNCHECKED_CAST")
    private fun getConverter(sourceKlass: KClass<*>, targetKlass: KClass<*>, errorDetails: (() -> String)? = null): ConvFn? {
        val convFn = getBaseConverter(sourceKlass, targetKlass)
        if (convFn != null)
            return convFn

        // for unknown sources (Map, Builder) use runtime converter
        if (sourceKlass == Any::class)
            return runtimeConverter(targetKlass)

        if (laMapperConfig.autoConvertValueClass) {
            // for value classes try more combination (value to/from simple)
            val sourceUnwrapped = getCustomUnwrappedTypeOrNull(sourceKlass)
            if (sourceUnwrapped != null) {
                val fn = getBaseConverter(sourceUnwrapped, targetKlass)
                if (fn != null)
                    return wrapSourceValueClassConverter(sourceKlass as KClass<Any>, fn)
            }

            val targetUnwrapped = getCustomUnwrappedTypeOrNull(targetKlass)
            if (targetUnwrapped != null && targetUnwrapped != String::class) { // laConverter can convert anything to String. Exclude this case for value class
                val fn = getBaseConverter(sourceKlass, targetUnwrapped)
                if (fn != null)
                    return wrapTargetValueClassConverter(targetKlass as KClass<Any>, fn)
            }

            if (laMapperConfig.autoConvertValueValue) {
                if (sourceUnwrapped != null && targetUnwrapped != null) {
                    val fn = getBaseConverter(sourceUnwrapped, targetUnwrapped)
                    if (fn != null)
                        return wrapSourceAndTargetValueClassConverter(sourceKlass as KClass<Any>, targetKlass as KClass<Any>, fn)
                }
            }
        }

        throw LaConvertException("Convert case is not defined (sourceType='$sourceKlass' targetType='$targetKlass' ${errorDetails?.invoke() ?: ""})")
    }

    private fun wrapNotNullConverter(convFn: ConvFn?, targetKlass: KClass<*>): ConvFn? {
        val nn = convertNull(targetKlass) ?: return convFn
        if (convFn == null)
            return ITypeConverter { it ?: nn }
        return ITypeConverter { convFn.convert(it) ?: nn }
    }

    internal fun getNullPrimaryConverter(targetKlass: KClass<*>, isMarkedNullable: Boolean): ConvFn {
        if (isMarkedNullable)
            return noConvertConverter
        val nn = convertNull(targetKlass) ?: return noConvertConverter
        return ITypeConverter { it ?: nn }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> convertValue(value: Any?, targetKlass: KClass<T>, nullable: Boolean = true): T? {
        if (value == null)
            return if (nullable) null else convertNull(targetKlass) as T?
        val convFn = getConverter(value::class, targetKlass)
        return convFn?.convert(value) as T?
    }

    @Suppress("UNCHECKED_CAST")
    private fun getBaseConverter(sourceKlass: KClass<*>, targetKlass: KClass<*>): ConvFn? {
        if (sourceKlass == targetKlass)
            return noConvertConverter
        try {
            val convFn = laConverterRegistry.getConverter(sourceKlass.java, targetKlass.java) as ConvFn?
            if (convFn != null)
                return convFn
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

    private fun runtimeConverter(targetKlass: KClass<out Any>): ConvFn {
        return ConvFn {
            if (it == null) {
                convertNull(targetKlass)
            } else {
                val conv = laConverterRegistry.getConverter(it::class.java, targetKlass.java)
                (conv as? ITypeConverter<Any, Any?>)?.convert(it)
            }
        }
    }

    private fun getCustomUnwrappedTypeOrNull(mainKlass: KClass<*>): KClass<*>? {
        if (!mainKlass.isValue)
            return null
        return mainKlass.primaryConstructor?.parameters?.singleOrNull()?.type?.classifier as KClass<*>?
    }

    private fun convertNull(klass: KClass<*>): Any? {
        if (klass.java == String::class.java)
            return if (laMapperConfig.autoConvertNullToString) "" else null
        if (!laMapperConfig.autoConvertNullForPrimitive)
            return null
        return convertPrimitiveNull(klass)
    }

    fun convertNull(targetType: KType): Any? {
        if (targetType.isMarkedNullable)
            return null
        val klass = targetType.classifier
        if (klass !is KClass<*>)
            return null
        return convertNull(klass)
    }

    companion object {
        internal val noConvertConverter: ConvFn = ITypeConverter { it }

        fun convertPrimitiveNull(klass: KClass<*>): Any? {
            when (klass) {
                Int::class -> return 0
                Long::class -> return 0L
                Boolean::class -> return false
                Char::class -> return '\u0000'
                Double::class -> return 0.0
                Float::class -> return 0.0f
                Short::class -> return 0
                Byte::class -> return 0
                UInt::class -> return 0
                ULong::class -> return 0L
                UByte::class -> return 0
                UShort::class -> return 0
            }
            return null
        }
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
