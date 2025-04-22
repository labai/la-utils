package com.github.labai.utils.mapper.impl

import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.impl.LaMapperImpl.AutoMapperImpl
import com.github.labai.utils.mapper.impl.LaMapperImpl.IMappingBuilderItem
import com.github.labai.utils.mapper.impl.LaMapperImpl.LambdaMapping
import com.github.labai.utils.mapper.impl.MappedStruct.ParamBind
import com.github.labai.utils.mapper.impl.MappedStruct.PropManualBind

/*
 * @author Augustus
 * created on 2025-04-18
 *
 * additional logic to solve closure lambdas.
 * Closure my have side effects if they are cached and later reused,
 * as they are created in some concrete context.
*/
internal object ClosureUtils {
    internal fun isClosure(lambda: Any): Boolean {
        return lambda.javaClass.declaredFields.any { field -> !field.isSynthetic }
    }

    internal fun <Fr : Any, To : Any> withOverrides(
        origMapper: AutoMapperImpl<Fr, To>,
        newMap: Map<String, IMappingBuilderItem<Fr>>,
        serviceContext: ServiceContext,
    ): AutoMapper<Fr, To> {
        val struct = WrapperMappedStruct(origMapper.struct, newMap)
        return ReflectionAutoMapper(struct, serviceContext)
    }

    // override closures in original struct
    private class WrapperMappedStruct<Fr : Any, To : Any>(
        private val orig: IMappedStruct<Fr, To>,
        private val newMap: Map<String, IMappingBuilderItem<Fr>>,
    ) : IMappedStruct<Fr, To> by orig {
        private var _paramBinds: Array<ParamBind<Fr>>? = null
        private var _propManualBinds: Array<PropManualBind<Fr, To>>? = null

        override val paramBinds: Array<ParamBind<Fr>>
            get() {
                if (_paramBinds == null) {
                    _paramBinds = if (orig.paramBinds.none { it.lambdaMapping?.isClosure == true }) {
                        // no closures - can reuse all
                        orig.paramBinds
                    } else {
                        orig.paramBinds.map { pb ->
                            if (pb.lambdaMapping == null || !pb.lambdaMapping.isClosure) {
                                pb
                            } else {
                                val newLambdaMapping = newMap[pb.param.name] as LambdaMapping<Fr>
                                pb.copy(newLambdaMapping)
                            }
                        }.toTypedArray()
                    }
                }
                return _paramBinds ?: error("unexpected null")
            }

        override val propManualBinds: Array<PropManualBind<Fr, To>>
            get() {
                if (_propManualBinds == null) {
                    _propManualBinds = if (orig.propManualBinds.none { it.lambdaMapping.isClosure }) {
                        // no closures - can reuse all
                        orig.propManualBinds
                    } else {
                        orig.propManualBinds.map { pb ->
                            if (!pb.lambdaMapping.isClosure) {
                                pb
                            } else {
                                val newLambdaMapping = newMap[pb.targetPropWr.name] as LambdaMapping<Fr>
                                val mixedLambdaMapping = pb.lambdaMapping.copy(newLambdaMapping.mapper)
                                PropManualBind(pb.targetPropWr, mixedLambdaMapping)
                            }
                        }.toTypedArray()
                    }
                }
                return _propManualBinds ?: error("unexpected null")
            }
    }
}
