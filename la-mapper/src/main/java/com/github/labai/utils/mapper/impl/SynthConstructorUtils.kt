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

import com.github.labai.utils.mapper.impl.MappedStruct.ParamBind
import java.lang.reflect.Constructor
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

/*
 * @author Augustus
 * created on 2023-02-22
 *
 * for constructor with optional (with default value) parameters
 * kotlin generates additional synthetic constructor with format:
 *   (param1, ..., paramN, flags, dummyParam)
 * where
 *   paramX - parameters, including optional
 *   flag - one (or few if needed) integers with bit marking provided params
 *   last param is dummy null
 *
*/
internal object SynthConstructorUtils {

    internal class SynthConConf<Fr : Any, To : Any>(
        val synthConstructor: Constructor<To>,
        val synthArgsTemplate: Array<Any?>,
        val paramBindWithHoles: List<ParamBind<Fr>?>, // list matches constructor params. null is for not provided optional param
    )

    internal fun <Fr : Any, To : Any> prepareSynthConParams(klass: KClass<To>, paramBinds: Array<ParamBind<Fr>>): SynthConConf<Fr, To>? {
        val targetConstructor: KFunction<To> = klass.primaryConstructor ?: return null
        val mappedBinds = paramBinds.associateBy { it.param.name }
        val params = targetConstructor.parameters

        val paramMappersWithHoles: MutableList<ParamBind<Fr>?> = mutableListOf()
        val flagCount: Int = 1 + (params.size - 1) / 32
        val paramArr = Array<Any?>(params.size + flagCount + 1) { null }

        if (params.isEmpty())
            return null

        // fill flags
        var currFlagNum = 0
        var currPos = 1u // single bit on first position
        var currFlag = 0u
        for ((i, param) in params.withIndex()) {
            val pmapper = mappedBinds[param.name]
            if (pmapper == null) {
                check(param.isOptional) { "param ${param.name} is not mapped and is not optional" }
                currFlag = (currFlag or currPos) // adding bit
                if (!param.type.isMarkedNullable) {
                    paramArr[i] = DataConverters.convertPrimitiveNull(param.type.classifier as KClass<*>) // if primitive, then convert to non-null
                }
            }
            if (i % 32 == 31) {
                paramArr[params.size + currFlagNum] = currFlag.toInt()
                currFlagNum++
                currFlag = 0u
                currPos = 1u
            } else {
                currPos = currPos * 2u // move bit by 1 position
            }
            paramMappersWithHoles.add(pmapper)
        }
        // save last part
        if (currPos != 1u) { // for 1 - didn't start yet
            paramArr[params.size + currFlagNum] = currFlag.toInt()
        }

        val synthConstructor = findSynthConstructor(klass) ?: return null

        return SynthConConf(
            synthConstructor = synthConstructor,
            synthArgsTemplate = paramArr,
            paramBindWithHoles = paramMappersWithHoles,
        )
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> findSynthConstructor(klass: KClass<T>): Constructor<T>? {
        val con = klass.java.constructors
            .lastOrNull { it.isSynthetic && it.parameterTypes[it.parameterTypes.size - 1] == DefaultConstructorMarker::class.java }
        return con as Constructor<T>?
    }
}
