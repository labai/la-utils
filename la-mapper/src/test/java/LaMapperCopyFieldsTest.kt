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
import com.github.labai.utils.mapper.LaMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * @author Augustus
 *         created on 2025.11.22
 */
class LaMapperCopyFieldsTest {

    companion object {
        internal const val ENGINES = "LaMapperCopyFieldsTest#validEngines"

        @JvmStatic
        fun validEngines(): Stream<String>? {
            return MappersConfig.engines()!!
                .filter { it != "full_compile" }
        }
    }

    class TestDto(val a0: String) {
        var a1: String = "a1"
        var a2: String = "a2"
        var a3: String = "a3"
        var a4: String = "a4"
    }

    @Test
    fun test1_copyFields_withExclude() {
        val (fr, to) = getTestDto()

        LaMapper.copyFields(fr, to) {
            exclude(t::a2)
        }
        assertEquals("a0-to", to.a0)
        assertEquals("a1-fr", to.a1)
        assertEquals("a2-to", to.a2)
    }

    @ParameterizedTest
    @MethodSource(ENGINES)
    fun test2_copyFields_excludeList(engine: String) {
        val (fr, to) = getTestDto()

        val mapper1 = MappersConfig.getFieldCopier<TestDto, TestDto>(engine) {
            exclude(t::a2, t::a3)
            exclude(t::a3)
            t::a2 from f::a0
            t::a4 from f::a3
        }
        mapper1.copyFields(fr, to)
        assertEquals("a0-to", to.a0)
        assertEquals("a1-fr", to.a1)
        assertEquals("a0-fr", to.a2)
        assertEquals("a3-to", to.a3)
        assertEquals("a3-fr", to.a4)
    }

    @ParameterizedTest
    @MethodSource(ENGINES)
    fun test3_copyFields_fromMap(engine: String) {
        val mapper = MappersConfig.getFieldCopier<Map<String, Any?>, TestDto>(engine) {
            exclude(t::a2)
            t::a4 from { "b" }
        }
        val from = mutableMapOf<String, Any?>("a1" to "a1-map")
        val to = TestDto("a").apply { a1 = "a1-to"; a2 = "a2-to"; a3 = "a3-to"; a4 = "a4-to" }
        mapper.copyFields(from, to)

        assertEquals("a1-map", to.a1)
        assertEquals("a2-to", to.a2) // exclude explicitly
        // If map doesn't contain value, we assume it is null.
        // As result, all fields of target class will be overwritten, except explicitly excluded
        assertEquals("", to.a3)
        assertEquals("b", to.a4)
    }

    @JvmRecord
    data class Dto4(
        val a1: String = "a1-x",
    )

    @ParameterizedTest
    @MethodSource(ENGINES)
    fun test4_copyFields_toRecord(engine: String) {
        val mapper = MappersConfig.getFieldCopier<Map<*, *>, Dto4>(engine)
        val from = mutableMapOf<String, Any?>("a1" to "a1-map")
        val to = Dto4("a1-to")
        mapper.copyFields(from, to)
        assertEquals("a1-to", to.a1) // not changed for records
    }

    class Parent5 {
        inner class Inner5 {
            var a1: String = "a1"
            var a2: String = "a2"
            var a3: String = "a3"
        }
        fun getInner() = Inner5()
    }

    @ParameterizedTest
    @MethodSource(ENGINES)
    fun test5_copyFields_innerClass(engine: String) {
        val mapper = MappersConfig.getFieldCopier<TestDto, Parent5.Inner5>(engine)
        val fr = TestDto("a0-fr").apply { a1 = "a1-fr"; a2 = "a2-fr" }
        val to = Parent5().getInner()
        mapper.copyFields(fr, to)
        assertEquals("a1-fr", to.a1)
        assertEquals("a2-fr", to.a2)
        assertEquals("a3", to.a3)
    }

    class Dto6(
        val a1: String = "a1-x",
        var a2: String = "a2-x",
        var a3: String = "a3-x",
    )

    @ParameterizedTest
    @MethodSource(ENGINES)
    fun test6_copyFields_params(engine: String) {
        val mapper = MappersConfig.getFieldCopier<Dto6, Dto6>(engine)
        val fr = Dto6(a1 = "a1-fr", a2 = "a2-fr", a3 = "a3-fr")
        val to = Dto6(a1 = "a1-to", a2 = "a2-to", a3 = "a3-to")
        mapper.copyFields(fr, to)
        assertEquals("a1-to", to.a1) // immutable
        assertEquals("a2-fr", to.a2)
        assertEquals("a3-fr", to.a3)
    }

    private fun getTestDto(): Pair<TestDto, TestDto> {
        val fr = TestDto("a0-fr").apply { a1 = "a1-fr"; a2 = "a2-fr"; a3 = "a3-fr"; a4 = "a4-fr" }
        val to = TestDto("a0-to").apply { a1 = "a1-to"; a2 = "a2-to"; a3 = "a3-to"; a4 = "a4-to" }
        return Pair(fr, to)
    }
}
