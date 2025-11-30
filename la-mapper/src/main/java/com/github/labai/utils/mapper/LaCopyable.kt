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
package com.github.labai.utils.mapper

import com.github.labai.utils.mapper.LaMapper.MappingBuilder

/*
 * @author Augustus
 * created on 2025-11-24
 *
 * Can be used for cloning objects
 *
 * Example:
 *  class Dto(val a1: String, var a2: String) : LaCopyable
 *  val acopy: Dto = dto.laCopy()
 *
*/
interface LaCopyable

inline fun <reified T : Any> LaCopyable.laCopy(noinline mapping: (MappingBuilder<T, T>.() -> Unit)? = null): T {
    check(this is T) { "both sides must be of same type for copying (${this::class} vs ${T::class})" }
    return LaMapper.copyFrom(this, mapping)
}
