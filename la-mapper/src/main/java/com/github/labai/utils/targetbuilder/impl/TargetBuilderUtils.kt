/*
The MIT License (MIT)

Copyright (c) 2025 Augustus

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
package com.github.labai.utils.targetbuilder.impl

import com.github.labai.utils.mapper.impl.ISourceStruct
import com.github.labai.utils.mapper.impl.PropAccessUtils.PropertyReader
import kotlin.collections.get
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/*
 * @author Augustus
 * created on 2025-06-28
*/
internal class PojoAsArray<T>(array: Array<T?>) {
    @Suppress("PropertyName") // normal name accidentally may be mapped by automap
    internal val __values = array
}

internal interface INameIndexResolver {
    fun getIndex(name: String?): Int
}

internal open class PojoAsArraySourceStruct<T : Any>(
    private var valueKlass: KClass<T>,
) : ISourceStruct<PojoAsArray<T?>> {
    private var idx: Int = 0
    private val map: MutableMap<String, PropertyReader<PojoAsArray<T?>>> = mutableMapOf()

    internal fun names() = map.keys.toList()

    @Suppress("UNCHECKED_CAST")
    override val type: KClass<PojoAsArray<T?>>
        get() = PojoAsArray::class as KClass<PojoAsArray<T?>>

    override fun get(name: String?): PropertyReader<PojoAsArray<T?>>? {
        if (name == null)
            return null
        return map.computeIfAbsent(name) {
            PojoAsArrayReader(name, idx++)
        }
    }

    internal inner class PojoAsArrayReader(name: String, var idx: Int) : PropertyReader<PojoAsArray<T?>>(name) {
        override val returnType: KType
            get() = valueKlass.createType()

        override fun getValue(pojo: PojoAsArray<T?>): Any? {
            return pojo.__values[idx]
        }

        override fun isFieldOrAccessor() = false
    }
}

internal class StringArraySourceStruct : PojoAsArraySourceStruct<String>(String::class)

internal class ObjectArraySourceStruct : PojoAsArraySourceStruct<Any>(Any::class)

internal class NameIndexResolver(names: List<String>) : INameIndexResolver {
    private val indexes = HashMap<String, Int>((names.size * 1.5).toInt())

    init {
        for ((i, name) in names.withIndex()) {
            indexes.put(name, i)
        }
    }

    override fun getIndex(name: String?): Int {
        return indexes[name] ?: -1
    }
}
