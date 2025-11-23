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
import com.github.labai.utils.mapper.impl.MappedStruct.ParamBind
import com.github.labai.utils.mapper.impl.MappedStruct.PropAutoBind
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

    internal interface Compiled3AutoMapper<Fr : Any, To : Any> : IAutoMapping<Fr, To>

    @Suppress("UNCHECKED_CAST")
    internal fun <Fr : Any, To : Any> compiledMapper(struct: MappedStruct<Fr, To>): IAutoMapping<Fr, To> {
        var synthConConf: SynthConConf<Fr, To>? = null
        if (struct.targetType.java.isRecord) {
            // all params are mandatory for record
        } else if (struct.targetConstructor != null) {
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
                } else {
                    prm.toHardCopyArgDef()
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
            struct.paramBinds.map { it.toHardCopyArgDef() }
        }

        val propDefsAut = struct.propAutoBinds.map { it.toHardCopyPropDef() }

        val propDefsMan = struct.propManualBinds.map {
            PojoCopyPropDef.forManual(it.lambdaMapping.mapper, it.targetPropWr.toNameOrAccessor(), it.lambdaMapping.convNnFn)
        }

        val copier = LaHardCopy.createPojoCopierClass<Fr, To>(
            struct.sourceStruct.type.java,
            struct.targetType.java,
            argDefs,
            propDefsAut + propDefsMan,
        )

        return object : Compiled3AutoMapper<Fr, To> {
            override fun transform(from: Fr): To {
                return copier.copyPojo(from) as To
            }
            override fun copyFields(from: Fr, to: To) {
                error("Should not use compile3 for copyFields")
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <Fr : Any> ParamBind<Fr>.toHardCopyArgDef(): PojoArgDef {
    val clazz = (this.param.type.classifier as KClass<*>).java
    return if (this.sourcePropRd != null) {
        if (this.sourcePropRd.isFieldOrAccessor()) {
            PojoArgDef.forProp(clazz, this.sourcePropRd.toNameOrAccessor(), this.convFn)
        } else { // used for "TargetBuilder"
            PojoArgDef.forSupplier(clazz, { fr: Fr -> this.sourcePropRd.getValue(fr) } as ((Any) -> Any)?, this.convFn)
        }
    } else {
        PojoArgDef.forSupplier(clazz, this.lambdaMapping!!.mapper as ((Any) -> Any)?, this.convFn)
    }
}

internal fun <Fr : Any, To : Any> PropAutoBind<Fr, To>.toHardCopyPropDef(): PojoCopyPropDef {
    return if (this.sourcePropRd.isFieldOrAccessor()) {
        PojoCopyPropDef.forAuto(this.sourcePropRd.toNameOrAccessor(), this.targetPropWr.toNameOrAccessor(), this.convNnFn)
    } else { // used for "TargetBuilder"
        PojoCopyPropDef.forManual<Fr>({ this.sourcePropRd.getValue(it) }, this.targetPropWr.toNameOrAccessor(), this.convNnFn)
    }
}
