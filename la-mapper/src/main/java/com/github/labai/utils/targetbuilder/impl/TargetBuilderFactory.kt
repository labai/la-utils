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

import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.impl.LaMapperAsmCompiler3
import com.github.labai.utils.mapper.impl.MappedStructFactory
import com.github.labai.utils.mapper.impl.ServiceContext
import com.github.labai.utils.targetbuilder.ITargetBuilderFactory
import kotlin.reflect.KClass

/*
 * @author Augustus
 * created on 2025-06-30
 *
 * build a target object from any kind of values
 * (as type is not known, all conversions will be on the fly)
*/
internal class TargetBuilderFactory<To : Any>(
    targetKlass: KClass<To>,
    serviceContext: ServiceContext,
) : ITargetBuilderFactory<To> {

    private val nameIndexResolver: INameIndexResolver
    private val mapper: AutoMapper<PojoAsArray<Any?>, To>
    private val arraySize: Int

    init {
        val structUtils = MappedStructFactory(serviceContext)
        val laMapperAsmCompiler3 = LaMapperAsmCompiler3(serviceContext)
        val sourceStruct = ObjectArraySourceStruct()
        val mStruct = structUtils.createMappedStruct(
            sourceStruct,
            targetKlass,
        )
        mapper = laMapperAsmCompiler3.compiledMapper(mStruct)
        arraySize = mStruct.paramBinds.size + mStruct.propAutoBinds.size + mStruct.propManualBinds.size
        nameIndexResolver = NameIndexResolver(sourceStruct.names())
    }

    inner class Builder : ITargetBuilderFactory.IBuilder<To> {
        private val values: Array<Any?> = arrayOfNulls(arraySize)

        override fun add(name: String?, value: Any?): Builder {
            val idx = nameIndexResolver.getIndex(name)
            if (idx >= 0)
                values[idx] = value
            return this
        }

        override fun build(): To {
            return mapper.transform(PojoAsArray(values))
        }
    }

    override fun instance(): ITargetBuilderFactory.IBuilder<To> {
        return Builder()
    }
}
