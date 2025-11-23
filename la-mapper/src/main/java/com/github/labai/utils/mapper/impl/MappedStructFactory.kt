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

import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.LaMapperImpl.LambdaMapping
import com.github.labai.utils.mapper.impl.LaMapperImpl.PropMapping
import com.github.labai.utils.mapper.impl.PropAccessUtils.PropertyReader
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

/*
 * @author Augustus
 * created on 2025-06-28
*/
internal class MappedStructFactory(private val serviceContext: ServiceContext) {
    private val config: LaMapperConfig = serviceContext.config

    //
    // for source as KClass
    //
    // if sourceType is Map, then use map values as the source instead of methods or fields
    //
    @Suppress("UNCHECKED_CAST")
    internal fun <Fr : Any, To : Any> createMappedStruct(
        sourceType: KClass<Fr>,
        targetType: KClass<To>,
        propMappers: Map<String, PropMapping<Fr>> = mapOf(),
        manualMappers: Map<String, LambdaMapping<Fr>> = mapOf(),
        hasClosure: Boolean = false,
        excludedFields: Set<String> = setOf(),
        skipObjectCreation: Boolean = false,
    ): MappedStruct<Fr, To> {
        val sourceStruct: ISourceStruct<Fr> = if (sourceType.isSubclassOf(Map::class)) {
            MapSourceStruct(sourceType as KClass<Map<String, Any?>>) as ISourceStruct<Fr>
        } else {
            ClassSourceStruct(sourceType)
        }
        if (targetType.isSubclassOf(Map::class))
            error("Mapping to Map is not supported yet")

        return MappedStruct(
            sourceStruct,
            targetType,
            propMappers,
            manualMappers,
            serviceContext,
            hasClosure,
            excludedFields,
            skipObjectCreation,
        )
    }

    // for source as ISourceStruct
    internal fun <Fr : Any, To : Any> createMappedStruct(
        sourceStruct: ISourceStruct<Fr>,
        targetType: KClass<To>,
        propMappers: Map<String, PropMapping<Fr>> = mapOf(),
        manualMappers: Map<String, LambdaMapping<Fr>> = mapOf(),
        hasClosure: Boolean = false,
    ): MappedStruct<Fr, To> {
        return MappedStruct(
            sourceStruct,
            targetType,
            propMappers,
            manualMappers,
            serviceContext,
            hasClosure,
        )
    }

    private fun <Fr : Any> getSourceMemberProps(sourceType: KClass<Fr>): List<PropertyReader<Fr>> {
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

    private inner class ClassSourceStruct<Fr : Any>(
        override val type: KClass<Fr>,
    ) : ISourceStruct<Fr> {
        private val sourcePropsByName: Map<String, PropertyReader<Fr>>
        init {
            sourcePropsByName = getSourceMemberProps(type)
                .associateBy { it.name }
        }
        override fun get(name: String?): PropertyReader<Fr>? {
            return if (name == null) null else sourcePropsByName[name]
        }
    }

    private inner class MapSourceStruct<Fr : Map<String, Any?>>(
        override val type: KClass<Fr>,
    ) : ISourceStruct<Fr> {
        private val readerMap: MutableMap<String, PropertyReader<Fr>> = mutableMapOf()
        override fun get(name: String?): PropertyReader<Fr>? {
            if (name == null)
                return null
            return readerMap.computeIfAbsent(name) {
                MapPropertyReader(name)
            }
        }
    }

    private class MapPropertyReader<T : Map<String, Any?>>(name: String) : PropertyReader<T>(name) {
        override val returnType: KType
            get() = ANY_KTYPE

        override fun getValue(pojo: T): Any? = pojo[name]

        override fun isFieldOrAccessor() = false
    }

    companion object {
        private val ANY_KTYPE = Any::class.createType()
    }
}
