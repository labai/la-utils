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

import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.MappedStruct.ParamBind
import com.github.labai.utils.mapper.impl.SynthConstructorUtils.SynthConConf
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance

/*
 * @author Augustus
 * created on 2023-02-21
 *
 * default reflection based transformer
 *
 * Pros
 *  - universal, safe
 * Cons
 *  - slower than handwritten ar compiled versions
 *
 * is default mapper for start
*/
internal class ReflectionAutoMapper<Fr : Any, To : Any>(
    private val struct: IMappedStruct<Fr, To>,
    serviceContext: ServiceContext,
) : IAutoMapping<Fr, To> {
    private val objectCreator: ObjectCreator<Fr, To> = ObjectCreator(struct.targetType, struct.targetConstructor, struct.paramBinds, serviceContext.config)

    override fun transform(from: Fr): To {
        val target: To = objectCreator.createObject(from)
        processAutoFields(from, target)
        processManualFields(from, target)
        return target
    }

    override fun copyFields(from: Fr, to: To) {
        processAutoFields(from, to)
        processManualFields(from, to)
    }

    // ordinary (non-constructor) fields, auto mapped
    private fun processAutoFields(from: Fr, target: To) {
        var i = -1
        val size = struct.propAutoBinds.size
        while (++i < size) {
            val propMapper = struct.propAutoBinds[i]
            val valTo = propMapper.sourcePropRd.getValue(from)
            val valConv = propMapper.convNnFn.convertValNn(valTo)
            propMapper.targetPropWr.setValue(target, valConv)
        }
    }

    // ordinary (non-constructor) fields, manually mapped
    private fun processManualFields(from: Fr, target: To) {
        var i = -1
        val size = struct.propManualBinds.size
        while (++i < size) {
            val mapr = struct.propManualBinds[i]
            val valTo = mapr.lambdaMapping.mapper(from)
            val valConv = mapr.lambdaMapping.convNnFn.convertValNn(valTo)
            mapr.targetPropWr.setValue(target, valConv)
        }
    }
}

// create object 'targetType' by paramMapper
// with some optimization based on constructor type
internal open class ObjectCreator<Fr : Any, To : Any>(
    private val targetType: KClass<To>,
    private val targetConstructor: KFunction<To>?,
    private val paramBinds: Array<ParamBind<Fr>>,
    private val config: LaMapperConfig,
) {
    // for case with provided all args
    private val allArgsNullsTemplate: Array<Any?>?

    // for case with optional args, but enabled synthetic constructor call hack
    private val synthConConf: SynthConConf<Fr, To>?
    private var disableSynthCall = false

    init {
        if (targetConstructor != null && paramBinds.size == targetConstructor.parameters.size) {
            allArgsNullsTemplate = arrayOfNulls(targetConstructor.parameters.size)
            synthConConf = null
        } else if (targetConstructor != null && !config.disableSyntheticConstructorCall) {
            allArgsNullsTemplate = null
            synthConConf = SynthConstructorUtils.prepareSynthConParams(targetType, paramBinds)
        } else {
            allArgsNullsTemplate = null
            synthConConf = null
        }
    }

    open fun createObject(from: Fr): To {
        val target: To = if (targetConstructor == null) {
            targetType.createInstance()
        } else if (allArgsNullsTemplate != null) {
            // args as array are slightly faster
            val paramArr = if (allArgsNullsTemplate.isNotEmpty()) allArgsNullsTemplate.clone() else EMPTY_ARRAY
            var i = -1
            val size = paramBinds.size
            while (++i < size) {
                paramArr[i] = paramBinds[i].mapParam(from)
            }
            targetConstructor.call(*paramArr)
        } else {
            // with optional args
            if (synthConConf != null && !disableSynthCall) {
                val paramArr = synthConConf.synthArgsTemplate.clone()
                var i = -1
                val size = paramBinds.size
                while (++i < size) {
                    val param = synthConConf.paramBindWithHoles[i] ?: continue
                    paramArr[i] = param.mapParam(from)
                }
                try {
                    synthConConf.synthConstructor.newInstance(*paramArr)
                } catch (e: IllegalArgumentException) {
                    if (!config.failOnOptimizationError && e.stackTrace.isNotEmpty() && e.stackTrace[0].className.startsWith("sun.reflect.")) { // ensure, it is invocation problem
                        LaMapper.logger.debug("Failed call $targetType (args: ${paramArr.joinToString(", ")}; params: ${synthConConf.synthConstructor.parameters.joinToString(", ")}")
                        disableSynthCall = true
                        // retry with native kotlin call
                        return createObject(from)
                    }
                    LaMapper.logger.debug("Failed call $targetType (args: ${paramArr.joinToString(", ")}; params: ${synthConConf.synthConstructor.parameters.joinToString(", ")}")
                    throw e
                }
            } else {
                val params = paramBinds.associate {
                    it.param to it.mapParam(from)
                }
                targetConstructor.callBy(params)
            }
        }
        return target
    }

    companion object {
        val EMPTY_ARRAY: Array<Any?> = arrayOf()
    }
}
