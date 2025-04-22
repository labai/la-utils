/*
The MIT License (MIT)

Copyright (c) 2023 Augustus

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

import com.github.labai.utils.convert.ITypeConverter
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.LaMapperImpl.LambdaMapping
import com.github.labai.utils.mapper.impl.LaMapperImpl.PropMapping
import com.github.labai.utils.mapper.impl.MappedStruct.ParamBind
import com.github.labai.utils.mapper.impl.MappedStruct.PropAutoBind
import com.github.labai.utils.mapper.impl.MappedStruct.PropManualBind
import com.github.labai.utils.mapper.impl.PropAccessUtils.PropertyReader
import com.github.labai.utils.mapper.impl.PropAccessUtils.PropertyWriter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod

/*
 * @author Augustus
 *         created on 2022.11.15
 *
 * some internal representation of mapping.
 *
 * for internal usage.
 */
internal interface IMappedStruct<Fr : Any, To : Any> {
    val sourceType: KClass<Fr>
    val targetType: KClass<To>
    val targetConstructor: KFunction<To>?
    val paramBinds: Array<ParamBind<Fr>>
    val propAutoBinds: Array<PropAutoBind<Fr, To>>
    val propManualBinds: Array<PropManualBind<Fr, To>>
}

internal class MappedStruct<Fr : Any, To : Any>(
    override val sourceType: KClass<Fr>,
    override val targetType: KClass<To>,
    private val propMappers: Map<String, PropMapping<Fr>>,
    private val manualMappers: Map<String, LambdaMapping<Fr>>,
    serviceContext: ServiceContext,
    internal val hasClosure: Boolean = false,
) : IMappedStruct<Fr, To> {
    override val targetConstructor: KFunction<To>?
    override val paramBinds: Array<ParamBind<Fr>>
    override val propAutoBinds: Array<PropAutoBind<Fr, To>>
    override val propManualBinds: Array<PropManualBind<Fr, To>>
    internal var areAllParams: Boolean = false
        private set
    private val dataConverters: DataConverters = serviceContext.dataConverters
    private val config: LaMapperConfig = serviceContext.config

    init {

        targetConstructor = if (targetType.java.isRecord) {
            RecordKConstructor.forRecordOrNull(targetType.java)
            // primaryConstructor may cause an exception with record and primitives (KT-58649)
        } else {
            targetType.primaryConstructor
        }

        val sourcePropsByName: Map<String, PropertyReader<Fr>> = getSourceMemberProps(sourceType)
            .associateBy { it.name }
        val targetFieldMap: Map<String, PropertyWriter<To>> = getTargetMemberProps(targetType)
            .associateBy { it.name }
        val targetArgsMandatory: Array<KParameter> = targetConstructor?.parameters
            ?.filter { it.name in manualMappers || it.name in propMappers || it.name in sourcePropsByName || !it.isOptional }
            ?.toTypedArray()
            ?: arrayOf() // may be null for Java classes - then will use no-arg constructor

        val autoProp = initPropAutoMappers(sourceType, targetType, skip = manualMappers.keys + propMappers.keys + targetArgsMandatory.mapNotNull { it.name })
        val manProp = initPropManualMappers(sourceType, targetType, propMappers)
        propAutoBinds = (autoProp + manProp).toTypedArray()

        propManualBinds = manualMappers.filter { arg -> targetConstructor?.parameters?.none { it.name == arg.key } ?: true }
            .filter { it.key in targetFieldMap }
            .map { PropManualBind(targetFieldMap[it.key]!!, it.value) }
            .toTypedArray()

        initManualMapperDataConverters(targetFieldMap, dataConverters)

        paramBinds = targetArgsMandatory.mapNotNull { param ->
            val manMapper = manualMappers[param.name]
            if (manMapper != null) {
                ParamBind(param, manMapper, null, manMapper.convNnFn, dataConverters)
            } else {
                val frName = propMappers[param.name]?.frName ?: param.name
                val prop = sourcePropsByName[frName]
                if (prop == null && !param.isOptional)
                    throw IllegalArgumentException("Parameter '${param.name}' is missing")
                if (prop == null) {
                    null
                } else {
                    val sourceKlass: KClass<*> = prop.klass
                    val targetKlass: KClass<*> = param.type.classifier as KClass<*>
                    val convFn = dataConverters.getConverterNn(sourceKlass, targetKlass, param.type.isMarkedNullable) { "sourceField=${prop.name} param=${param.name}" }
                    ParamBind(param, null, prop, convFn, dataConverters)
                }
            }
        }.toTypedArray()

        areAllParams = targetConstructor != null && paramBinds.size == targetConstructor.parameters.size
    }

    // for constructor parameters
    internal class ParamBind<Fr>(
        internal val param: KParameter,
        internal val lambdaMapping: LambdaMapping<Fr>?,
        internal val sourcePropRd: PropertyReader<Fr>?, // may be replaced to compiled
        internal val convFn: ConvFn?,
        internal val dataConverters: DataConverters,
    ) {
        private val paramType = param.type
        private val paramKlass = paramType.classifier as KClass<*>

        internal fun mapParam(from: Fr): Any? {
            val value = if (sourcePropRd != null) {
                sourcePropRd.getValue(from)
            } else if (lambdaMapping != null) {
                lambdaMapping.mapper(from)
            } else {
                throw NullPointerException("ParamMapper must have manualMapper or sourceProp not null")
            }
            return if (lambdaMapping != null && lambdaMapping.sourceType == null) { // lambdas with unknown return type - convert based on return result
                dataConverters.convertValue(value, paramKlass)
            } else {
                convFn.convertValOrNull(value)
            } ?: dataConverters.convertNull(param.type)
        }

        internal fun copy(newLambdaMapping: LambdaMapping<Fr>): ParamBind<Fr> {
            return ParamBind(param, newLambdaMapping, sourcePropRd, convFn, dataConverters)
        }
    }

    internal class PropAutoBind<Fr, To>(
        val sourcePropRd: PropertyReader<Fr>,
        val targetPropWr: PropertyWriter<To>,
        val convNnFn: ConvFn?,
    )

    internal class PropManualBind<Fr : Any, To : Any>(
        val targetPropWr: PropertyWriter<To>,
        val lambdaMapping: LambdaMapping<Fr>,
    )

    private fun initManualMapperDataConverters(targetFieldMap: Map<String, PropertyWriter<To>>, dataConverters: DataConverters) {
        val paramMap = targetConstructor?.parameters?.associateBy { it.name } ?: mapOf()

        for ((name, manMapper) in manualMappers) {
            val targetType = paramMap[name]?.type   // constructor param
                ?: targetFieldMap[name]?.returnType // property
                ?: continue
            if (targetType.classifier !is KClass<*>)
                continue
            val targetKlass: KClass<*> = targetType.classifier as KClass<*>
            val targetNullable: Boolean = targetType.isMarkedNullable

            if (manMapper.sourceType == null) { // use on-the-fly converter based on return value
                manMapper.convNnFn = ITypeConverter { value ->
                    dataConverters.convertValue(value, targetKlass, targetNullable)
                }
            } else {
                if (manMapper.sourceType.classifier !is KClass<*>)
                    continue
                val sourceKlass: KClass<*> = manMapper.sourceType.classifier as KClass<*>
                manMapper.targetType = targetType
                manMapper.convNnFn = dataConverters.getConverterNn(sourceKlass, targetKlass, targetType.isMarkedNullable) { "field=$name" }
            }
        }
    }

    private fun getSourceMemberProps(sourceType: KClass<Fr>): List<PropertyReader<Fr>> {
        return sourceType.memberProperties
            .mapNotNull {
                if (it.visibility in config.visibilities)
                    PropAccessUtils.resolvePropertyReader(it.name, it, null)
                else {
                    val getter: KFunction<*>? = PropAccessUtils.getGetterByName(sourceType, it.name, it.returnType) // case for java getters
                    if (getter != null && getter.visibility in config.visibilities)
                        PropAccessUtils.resolvePropertyReader(it.name, null, getter)
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
                    PropAccessUtils.resolvePropertyWriter(it.name, it as KMutableProperty1<To, Any?>, null)
                else { // try check case for java setters
                    val setter: KFunction<*>? = PropAccessUtils.getSetterByName(targetType, it.name, it.returnType)
                    if (setter != null && setter.visibility in config.visibilities)
                        PropAccessUtils.resolvePropertyWriter(it.name, null, setter)
                    else
                        null
                }
            }
    }

    private fun initPropAutoMappers(
        sourceClass: KClass<Fr>,
        targetClass: KClass<To>,
        skip: Set<String>,
    ): List<PropAutoBind<Fr, To>> {
        val propsFr: Map<String, PropertyReader<Fr>> = getSourceMemberProps(sourceClass)
            .filterNot { it.name in skip }
            .associateBy { it.name }

        return getTargetMemberProps(targetClass)
            .filter { it.name in propsFr }
            .map {
                val pfr = propsFr[it.name]!!
                val convNnFn = dataConverters.getConverterNn(pfr.klass, it.klass, it.returnType.isMarkedNullable)
                PropAutoBind(
                    sourcePropRd = pfr,
                    targetPropWr = it,
                    convNnFn = convNnFn,
                )
            }
    }

    private fun initPropManualMappers(
        sourceClass: KClass<Fr>,
        targetClass: KClass<To>,
        propMappers: Map<String, PropMapping<Fr>>,
    ): List<PropAutoBind<Fr, To>> {
        val frPropNames = propMappers.values.map { it.frName }.toSet()
        val toPropNames = propMappers.keys
        val propsFr: Map<String, PropertyReader<Fr>> = getSourceMemberProps(sourceClass)
            .filter { it.name in frPropNames }
            .associateBy { it.name }
        return getTargetMemberProps(targetClass)
            .filter { it.name in toPropNames }
            .map {
                val pfr = propsFr[propMappers[it.name]!!.frName]!!
                val convNnFn = dataConverters.getConverterNn(pfr.klass, it.klass, it.returnType.isMarkedNullable)
                PropAutoBind(
                    sourcePropRd = pfr,
                    targetPropWr = it,
                    convNnFn = convNnFn,
                )
            }
    }
}

// property readers and writers
internal object PropAccessUtils {

    internal abstract class PropertyReader<T>(
        val name: String,
    ) {
        abstract val returnType: KType
        val klass: KClass<*>
            get() = returnType.classifier as KClass<*>

        abstract fun getValue(pojo: T): Any?
    }

    internal class PropertyReaderProp<T>(name: String, internal val prop: KProperty1<T, Any?>) : PropertyReader<T>(name) {
        override val returnType: KType
            get() = prop.returnType

        override fun getValue(pojo: T): Any? {
            return prop.get(pojo)
        }
    }

    // for java getter functions (getField())
    internal class PropertyReaderGetter<T>(name: String, internal val getter: KFunction<Any?>) : PropertyReader<T>(name) {
        override val returnType: KType
            get() = getter.returnType

        override fun getValue(pojo: T): Any? {
            return getter.call(pojo)
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
    internal class PropertyWriterSetter<T>(name: String, internal val setter: KFunction<*>) : PropertyWriter<T>(name) {
        override val returnType: KType = setter.parameters.last().type

        override fun setValue(pojo: T, value: Any?) {
            setter.call(pojo, value)
        }
    }

    internal fun getGetterByName(sourceClass: KClass<*>, fieldName: String, type: KType): KFunction<*>? {
        if (fieldName.isEmpty())
            return null
        val fnName = "get" + fieldName[0].uppercaseChar() + fieldName.substring(1)
        return sourceClass.declaredFunctions.find { f -> f.name == fnName && f.returnType == type } // getField()
            ?: sourceClass.declaredFunctions.find { f -> f.name == fieldName && f.returnType == type } // also field()
    }

    internal fun getSetterByName(sourceClass: KClass<*>, fieldName: String, type: KType): KFunction<*>? {
        if (fieldName.isEmpty())
            return null
        val fnName = "set" + fieldName[0].uppercaseChar() + fieldName.substring(1)
        return sourceClass.declaredFunctions
            .find { it.name == fnName && it.parameters.size == 2 && it.parameters.last().type == type }
    }

    internal fun <T> resolvePropertyReader(
        name: String,
        prop: KProperty1<T, Any?>?,
        getter: KFunction<Any?>?, // for java getter functions (getField())
    ): PropertyReader<T> {
        return if (prop != null) {
            return PropertyReaderProp(name, prop)
        } else if (getter != null) {
            PropertyReaderGetter(name, getter)
        } else {
            throw NullPointerException("One of prop or getter is mandatory")
        }
    }

    internal fun <T> resolvePropertyWriter(
        name: String,
        prop: KMutableProperty1<T, Any?>?,
        setter: KFunction<*>?,
    ): PropertyWriter<T> {
        return if (prop != null) {
            return PropertyWriterProp(name, prop)
        } else if (setter != null) {
            PropertyWriterSetter(name, setter)
        } else {
            throw NullPointerException("One of prop or setter is mandatory")
        }
    }

    internal fun <T> PropertyReader<T>.toNameOrAccessor(): NameOrAccessor {
        return if (this is PropertyReaderProp) {
            NameOrAccessor.name(this.name)
        } else if (this is PropertyReaderGetter) {
            NameOrAccessor.accessor(this.getter.javaMethod)
        } else {
            error("Invalid PropertyReader")
        }
    }

    internal fun <T> PropertyWriter<T>.toNameOrAccessor(): NameOrAccessor {
        return if (this is PropertyWriterProp) {
            NameOrAccessor.name(this.name)
        } else if (this is PropertyWriterSetter) {
            NameOrAccessor.accessor(this.setter.javaMethod)
        } else {
            error("Invalid PropertyWriter")
        }
    }
}
