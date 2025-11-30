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
import com.github.labai.utils.mapper.LaCopyable
import com.github.labai.utils.mapper.laCopy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/*
 * @author Augustus
 * created on 2025-11-24
*/
class LaCopyableTest {

    class Sample(val a1: String) : LaCopyable {
        var a2: String? = "abc"
    }

    open class DtoPar1 : LaCopyable {
        var p1: String? = null
    }

    interface IDtoPar2 {
        var p2: String?
    }

    class Dto(val a1: String, var a2: String) : DtoPar1(), IDtoPar2 {
        var a3: String? = null
        override var p2: String? = "x"
    }

    @Test
    fun test_sample() {
        val sample = Sample("A1").apply {
            a2 = "A2"
        }
        val copy: Sample = sample.laCopy()
        assertEquals(sample.a1, copy.a1)
        assertEquals(sample.a2, copy.a2)
    }

    @Test
    fun test_lacopy_whenWithParent_copyAllFields() {
        val dto = Dto("a1-fr", "a2-fr").apply {
            a3 = "a3-fr"
            p1 = "p1-fr"
            p2 = "p2-fr"
        }
        val res: Dto = dto.laCopy()

        assertEquals(dto.a1, res.a1)
        assertEquals(dto.a2, res.a2)
        assertEquals(dto.a3, res.a3)
        assertEquals(dto.p1, res.p1)
        assertEquals(dto.p2, res.p2)
    }

    @Test
    fun test_lacopy_whenTargetParent_createParentOnly() {
        val dto = Dto("a1-fr", "a2-fr").apply {
            a3 = "a3-fr"
            p1 = "p1-fr"
            p2 = "p2-fr"
        }
        val res: DtoPar1 = dto.laCopy()
        assertEquals(dto.p1, res.p1)

        assertThrows<ClassCastException> { res as Dto }
    }

    @Test
    fun test_lacopy_copyWithMapping() {
        val dto = Dto("a1-fr", "a2-fr").apply {
            a3 = "a3-fr"
            p1 = "p1-fr"
            p2 = "p2-fr"
        }
        val res: Dto = dto.laCopy {
            t::a1 from f::a2
            t::a2 from { "abra" }
            t::p1 from f::p2
        }

        assertEquals(dto.a2, res.a1)
        assertEquals("abra", res.a2)
        assertEquals(dto.a3, res.a3)
        assertEquals(dto.p2, res.p1)
        assertEquals(dto.p2, res.p2)
    }
}
