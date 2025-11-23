/*
The MIT License (MIT)

Copyright (c) 2022 Augustus

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

import java.lang.reflect.Constructor
import java.lang.reflect.RecordComponent
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KParameter.Kind
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType

/*
 * @author Augustus
 * created on 2024-07-31
 *
 * hack for records.
 * Current kotlin implementation contains an issue
 * when an exception is thrown for record with primitive in argument (KT-58649)
 *
 * records still have some limitations:
 * - no 'compiling' if target is record
 * - use 't' and 'f' for field reference
 *
*/

internal class RecordKParam(
    private val rc: RecordComponent,
    private val idx: Int,
) : KParameter {
    override val annotations: List<Annotation>
        get() = listOf()
    override val index: Int
        get() = idx
    override val isOptional: Boolean
        get() = false
    override val isVararg: Boolean
        get() = false
    override val kind: Kind
        get() = INSTANCE
    override val name: String
        get() = rc.name
    override val type: KType by lazy {
        val nullable = !rc.type.isPrimitive // maybe also check @Nonnull/@Nullable?
        rc.type.kotlin.createType(List(rc.type.typeParameters.size) { KTypeProjection.STAR }, nullable)
    }
}

internal class RecordKConstructor<T>(
    private val cls: Class<T>,
    private val con: Constructor<T>,
) : KFunction<T> {
    private val params = getRecordKParams(cls).toList()

    override val isExternal: Boolean
        get() = false
    override val isInfix: Boolean
        get() = false
    override val isInline: Boolean
        get() = false
    override val isOperator: Boolean
        get() = false
    override val isSuspend: Boolean
        get() = false
    override val annotations: List<Annotation>
        get() = listOf()
    override val isAbstract: Boolean
        get() = false
    override val isFinal: Boolean
        get() = true
    override val isOpen: Boolean
        get() = false
    override val name: String
        get() = con.name
    override val parameters: List<KParameter>
        get() = params
    override val returnType: KType
        get() = dummy()
    override val typeParameters: List<KTypeParameter>
        get() = dummy()
    override val visibility: KVisibility
        get() = dummy()

    override fun call(vararg args: Any?): T {
        return con.newInstance(*args)
    }

    override fun callBy(args: Map<KParameter, Any?>): T {
        dummy()
    }

    private fun <T> getRecordKParams(cls: Class<T>): List<KParameter> {
        return cls.recordComponents
            .mapIndexed { i, p -> RecordKParam(p, i) }
    }

    private fun dummy(): Nothing = TODO("Not implemented (no need)")

    companion object {
        fun <T> forRecordOrNull(cls: Class<T>): RecordKConstructor<T>? {
            if (!cls.isRecord)
                return null
            val paramTypes = cls.recordComponents
                .map { it.type }
                .toTypedArray()
            val con: Constructor<T> = cls.getDeclaredConstructor(*paramTypes)
            return RecordKConstructor(cls, con)
        }
    }
}
