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

import com.github.labai.utils.hardreflect.LaHardReflect
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor
import com.github.labai.utils.hardreflect.PropMultiReader
import com.github.labai.utils.hardreflect.PropMultiWriter
import com.github.labai.utils.mapper.impl.MappedStruct.ParamBind
import com.github.labai.utils.mapper.impl.MappedStruct.PropAutoBind
import com.github.labai.utils.mapper.impl.MappedStruct.PropManualBind
import com.github.labai.utils.mapper.impl.PropAccessUtils.PropertyReader
import com.github.labai.utils.mapper.impl.PropAccessUtils.PropertyWriter
import com.github.labai.utils.mapper.impl.PropAccessUtils.toNameOrAccessor
import kotlin.reflect.KClass
import kotlin.reflect.KType

/*
 * @author Augustus
 * created on 2023-02-21
 *
 * Compiled version.
 *
 * 2nd version - partially compile - only access to object properties.
 *
 * - read source pojo values to array (one reader class)
 * - write to target pojo from an array (one writer class)
 * - reflection for 'value class' type
 * - reflection for manual assigns
 *
 * For compiling use ASM lib.
 *
 * Pros:
 *  - mapping performance is a few times faster than using reflection
 *
 * Cons:
 *  - still slower than handwritten assigns
 *
 * Is enabled by default - will be chosen when can't use full-copy generated class (LaMapperAsmCompiler2)
 *
 *
*/
internal class LaMapperAsmCompiler2(private val serviceContext: ServiceContext) {

    internal interface Compiled2AutoMapper<Fr : Any, To : Any> : IAutoMapping<Fr, To>

    internal fun <Fr : Any, To : Any> compiledMapper(struct: MappedStruct<Fr, To>): IAutoMapping<Fr, To> {
        // create on a reader object for params
        //
        val readableParams = struct.paramBinds.filter { it.sourcePropRd != null && isSuitableForCompile(it.sourcePropRd) }
        val propsPrmFr: List<NameOrAccessor> = readableParams.map { it.sourcePropRd!!.toNameOrAccessor() }
        val objectCreator: ObjectCreator<Fr, To>
        val multiReaderMainCon: MultiReaderMain<Fr>?
        if (propsPrmFr.isNotEmpty()) {
            val compiledReader = LaHardReflect.createMultiReaderClassForProps(struct.sourceStruct.type.java, propsPrmFr)
            multiReaderMainCon = MultiReaderMain(compiledReader)
            var pos = 0
            val compiledParamBind = struct.paramBinds.map { orig ->
                ParamBind(
                    param = orig.param,
                    lambdaMapping = orig.lambdaMapping,
                    sourcePropRd = if (orig in readableParams) multiReaderMainCon.SinglePropReader(orig.sourcePropRd!!, pos++) else orig.sourcePropRd,
                    convFn = orig.convFn,
                    dataConverters = orig.dataConverters,
                )
            }.toTypedArray()

            objectCreator = ObjectCreator(struct.targetType, struct.targetConstructor, compiledParamBind, serviceContext.config)
        } else { // nothing to read
            multiReaderMainCon = null
            objectCreator = ObjectCreator(struct.targetType, struct.targetConstructor, struct.paramBinds, serviceContext.config)
        }

        // ordinary (non-constructor) fields, auto mapped
        //
        val propsAutFr = struct.propAutoBinds
            .filter { isSuitableForCompile(it.sourcePropRd) }
            .map { it.sourcePropRd.toNameOrAccessor() }
        val propsAutTo = struct.propAutoBinds
            .filter { isClassSuitableForCompile(it.targetPropWr.klass) }
            .map { it.targetPropWr.toNameOrAccessor() }
        val multiReaderMainAut: MultiReaderMain<Fr>?
        val multiWriterMainAut: MultiWriterMain<To>?
        val compiledPropAutoBinds: Array<PropAutoBind<Fr, To>>
        if (propsAutFr.isNotEmpty() || propsAutTo.isNotEmpty()) {
            val compiledReader = if (propsAutFr.isEmpty()) null else LaHardReflect.createMultiReaderClassForProps(struct.sourceStruct.type.java, propsAutFr)
            val compiledWriter = if (propsAutTo.isEmpty()) null else LaHardReflect.createMultiWriterClassForProps(struct.targetType.java, propsAutTo)
            multiReaderMainAut = if (compiledReader == null) null else MultiReaderMain(compiledReader)
            multiWriterMainAut = if (compiledWriter == null) null else MultiWriterMain(compiledWriter, propsAutTo.size)
            var rpos = 0
            var wpos = 0
            compiledPropAutoBinds = struct.propAutoBinds.map { orig ->
                PropAutoBind(
                    sourcePropRd = multiReaderMainAut?.SinglePropReader(orig.sourcePropRd, rpos++) ?: orig.sourcePropRd,
                    targetPropWr = multiWriterMainAut?.SinglePropWriter(orig.targetPropWr, wpos++) ?: orig.targetPropWr,
                    convNnFn = orig.convNnFn,
                )
            }.toTypedArray()
        } else {
            multiReaderMainAut = null
            multiWriterMainAut = null
            compiledPropAutoBinds = struct.propAutoBinds
        }

        // ordinary (non-constructor) fields, manual mapped
        //
        val propsManTo = struct.propManualBinds
            .filter { isClassSuitableForCompile(it.targetPropWr.klass) }
            .map { it.targetPropWr.toNameOrAccessor() }
        val multiWriterMainMan: MultiWriterMain<To>?
        val compiledPropManualBinds: Array<PropManualBind<Fr, To>>
        if (propsManTo.isNotEmpty()) {
            val compiledWriter = LaHardReflect.createMultiWriterClassForProps(struct.targetType.java, propsManTo)
            multiWriterMainMan = MultiWriterMain<To>(compiledWriter, propsManTo.size)
            compiledPropManualBinds = struct.propManualBinds.mapIndexed { pos, orig ->
                PropManualBind(
                    targetPropWr = multiWriterMainMan.SinglePropWriter(orig.targetPropWr, pos),
                    lambdaMapping = orig.lambdaMapping,
                )
            }.toTypedArray()
        } else {
            multiWriterMainMan = null
            compiledPropManualBinds = struct.propManualBinds
        }

        return object : Compiled2AutoMapper<Fr, To> {
            override fun transform(from: Fr): To {
                multiReaderMainCon?.readValues(from)
                val target: To = objectCreator.createObject(from)
                processAutoFields(from, target)
                processManualFields(from, target)
                return target
            }

            override fun copyFields(from: Fr, to: To) {
                multiReaderMainCon?.readValues(from)
                processAutoFields(from, to)
                processManualFields(from, to)
            }

            // ordinary (non-constructor) fields, auto mapped
            private fun processAutoFields(from: Fr, target: To) {
                multiReaderMainAut?.readValues(from)
                var i = -1
                val size = compiledPropAutoBinds.size
                while (++i < size) {
                    val propMapper = compiledPropAutoBinds[i]
                    val valTo = propMapper.sourcePropRd.getValue(from)
                    val valConv = propMapper.convNnFn.convertValNn(valTo)
                    propMapper.targetPropWr.setValue(target, valConv)
                }
                multiWriterMainAut?.writeValues(target)
            }

            // ordinary (non-constructor) fields, manually mapped
            private fun processManualFields(from: Fr, to: To) {
                var i = -1
                val size = compiledPropManualBinds.size
                while (++i < size) {
                    val mapr = compiledPropManualBinds[i]
                    val valTo = mapr.lambdaMapping.mapper(from)
                    val valConv = mapr.lambdaMapping.convNnFn.convertValNn(valTo)
                    mapr.targetPropWr.setValue(to, valConv)
                }
                multiWriterMainMan?.writeValues(to)
            }
        }
    }

    // parent class - for all read props pack
    // first step - load read values to array of data (repeat for each pojo)
    // then each child reader takes its own value from array
    internal class MultiReaderMain<Fr : Any>(
        private val propMultiReader: PropMultiReader,
    ) {
        lateinit var data: Array<Any?>

        fun readValues(from: Fr) {
            this.data = propMultiReader.readVals(from)
        }

        internal inner class SinglePropReader<Fr : Any>(
            private val orig: PropertyReader<Fr>,
            private val pos: Int,
        ) : PropertyReader<Fr>(orig.name) {

            override val returnType: KType
                get() = orig.returnType

            override fun getValue(pojo: Fr): Any? {
                return data[pos]
            }
        }
    }

    internal class MultiWriterMain<To : Any>(
        private val propMultiWriter: PropMultiWriter,
        size: Int,
    ) {
        val data: Array<Any?> = Array(size) { null }

        // no need to clear, as we always rewrite all values (no option for skip)
        // fun cleanData() { }

        fun writeValues(to: To) {
            propMultiWriter.writeVals(to, data)
        }

        internal inner class SinglePropWriter<To : Any>(
            private val orig: PropertyWriter<To>,
            private val pos: Int,
        ) : PropertyWriter<To>(orig.name) {

            override val returnType: KType
                get() = orig.returnType

            override fun setValue(pojo: To, value: Any?) {
                data[pos] = value
            }
        }
    }

    private fun isClassSuitableForCompile(klass: KClass<*>): Boolean {
        if (klass.isValue) // skip value-classes
            return false
        return true
    }

    private fun <Fr : Any> isSuitableForCompile(reader: PropertyReader<Fr>): Boolean {
        if (!isClassSuitableForCompile(reader.klass))
            return false
        if (!reader.isFieldOrAccessor())
            return false
        return true
    }
}
