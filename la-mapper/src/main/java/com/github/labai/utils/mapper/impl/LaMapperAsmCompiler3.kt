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

import com.github.labai.utils.hardreflect.LaHardCopy
import com.github.labai.utils.hardreflect.LaHardCopy.PojoArgDef
import com.github.labai.utils.hardreflect.LaHardCopy.PojoCopyPropDef
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.impl.PropAccessUtils.toNameOrAccessor
import com.github.labai.utils.mapper.impl.SynthConstructorUtils.SynthConConf
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.reflect.KClass

/*
 * @author Augustus
 * created on 2023-02-21
 *
 * Compiled version.
 *
 * 3rd version - full compile - object creation and all reads, writes.
 *
 * For compiling use ASM lib.
 * Pros
 *  - mapping performance is similar to hand-mapped
 * Cons
 *  - doesn't cover few cases yet (value classes)
 *
 * Is enabled by default
 *
*/
internal class LaMapperAsmCompiler3(private val serviceContext: ServiceContext) {

    @Suppress("UNCHECKED_CAST")
    internal fun <Fr : Any, To : Any> compiledMapper(struct: MappedStruct<Fr, To>): AutoMapper<Fr, To> {
        var synthConConf: SynthConConf<Fr, To>? = null
        if (/*!struct.areAllParams &&*/ struct.targetConstructor != null) {
            synthConConf = SynthConstructorUtils.prepareSynthConParams(struct.targetType, struct.paramBinds)
        } else if (struct.targetType.java.constructors.none { it.parameterCount == 0 }) {
            throw IllegalArgumentException("Class ${struct.targetType} doesn't have no-argument constructor")
        }
        val argDefs = if (synthConConf != null) {
            checkNotNull(struct.targetConstructor)
            val argd = synthConConf.paramBindWithHoles.mapIndexed { i, prm ->
                if (prm == null) { // skipped param (use null or 0 for primitives)
                    val nullOr0 = synthConConf.synthArgsTemplate[i]
                    val p = struct.targetConstructor.parameters[i]
                    PojoArgDef.forConstant((p.type.classifier as KClass<*>).java, nullOr0)
                } else if (prm.sourcePropRd != null) {
                    PojoArgDef.forProp((prm.param.type.classifier as KClass<*>).java, prm.sourcePropRd.toNameOrAccessor(), prm.convFn)
                } else {
                    PojoArgDef.forSupplier((prm.param.type.classifier as KClass<*>).java, prm.manualMapping!!.mapper as ((Any) -> Any)?, prm.convFn)
                }
            }.toMutableList()
            // add addition params for kotlin synthetic constructor
            for (i in struct.targetConstructor.parameters.size until synthConConf.synthArgsTemplate.size) {
                val isLast = (i == synthConConf.synthArgsTemplate.size - 1)
                val extra = synthConConf.synthArgsTemplate[i]
                argd += if (isLast) { // last is dummy
                    PojoArgDef.forConstant(DefaultConstructorMarker::class.java, null)
                } else { // int - flags of provided arguments
                    PojoArgDef.forConstant(Int::class.java, extra)
                }
            }
            argd
        } else {
            struct.paramBinds.map {
                PojoArgDef.forProp((it.param.type.classifier as KClass<*>).java, NameOrAccessor.name(it.param.name), it.convFn)
            }
        }

        val propDefsAut = struct.propAutoBinds.map {
            PojoCopyPropDef.forAuto(it.sourcePropRd.toNameOrAccessor(), it.targetPropWr.toNameOrAccessor(), it.convNnFn)
        }

        val propDefsMan = struct.propManualBinds.map {
            PojoCopyPropDef.forManual(it.manualMapping.mapper, it.targetPropWr.toNameOrAccessor(), it.manualMapping.convNnFn)
        }

        val copier = LaHardCopy.createPojoCopierClass<Fr, To>(
            struct.sourceType.java,
            struct.targetType.java,
            argDefs,
            propDefsAut + propDefsMan,
        )

        return object : AutoMapper<Fr, To> {
            override fun transform(from: Fr): To {
                return copier.copyPojo(from) as To
            }
        }
    }
}
