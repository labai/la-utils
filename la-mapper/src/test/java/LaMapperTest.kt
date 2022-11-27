import TestStructJv.Test1Pojo
import TestStructJv.Test2PojoConstr
import com.github.labai.utils.mapper.LaMapper
import com.google.gson.GsonBuilder
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperTest {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    fun test_constructor_to_constructor() {

        data class From(
            val v1: Int,
            val v2: BigDecimal
        )

        class To(
            val v1: BigDecimal,
            val v2: BigDecimal
        )

        val mapper = LaMapper.autoMapper<From, To>()

        val from = From(5, BigDecimal("6.5"))

        val res = mapper.transform(from)

        assertEqBigDecimal("5", res.v1)
        assertEqBigDecimal("6.5", res.v2)

        // same with copyFrom()
        val res2: To = LaMapper.copyFrom(from)
        assertEqBigDecimal("5", res2.v1)
        assertEqBigDecimal("6.5", res2.v2)
    }

    @Test
    fun test_fields_assign() {

        class From {
            var a20: Int? = null
            var a30: LocalDate? = null
        }

        class To {
            var a20: Long? = null
            var a30: LocalDateTime? = null
        }

        val mapper = LaMapper.autoMapper<From, To>()

        val from = From().apply {
            a20 = 7
            a30 = LocalDate.parse("2022-11-08")
        }

        val res = mapper.transform(from)

        assertEquals(7L, res.a20)
        assertEquals(LocalDateTime.parse("2022-11-08T00:00:00"), res.a30)
    }

    @Test
    fun test_mixed_assign() {

        class From(val v1: Int, val v2: BigDecimal) {
            var v3: Int? = null
            var v4: LocalDate? = null
            var v5: Int? = null
        }

        class To(val v1: BigDecimal, var v3: Long? = null, val v5: Int) {
            var v2: BigDecimal? = null
            var v4: LocalDateTime? = null
            val v6: String = "justGetter"
        }

        val mapper = LaMapper.autoMapper<From, To>()

        val from = From(5, BigDecimal("6.5")).apply {
            v3 = 13
            v4 = LocalDate.parse("2022-11-08")
            v5 = 15
        }

        val res = mapper.transform(from)

        assertEqBigDecimal("5", res.v1)
        assertEqBigDecimal("6.5", res.v2!!)
        assertEquals(13, res.v3)
        assertEquals(15, res.v5)
        assertEquals(LocalDateTime.parse("2022-11-08T00:00:00"), res.v4)
    }

    @Test
    fun test_mappings_property_mapping() {

        class From {
            var v11: Int? = null
            var v12: Long? = null
        }

        class To {
            var v11: Long? = null
            var v13: String? = null
            var v14: String = "x"
        }

        val mapper = LaMapper.autoMapper<From, To> {
            To::v11 from { it.v11!! * 2 }
            To::v13 from From::v12 // should be auto-convert by types as well (Long->String)
        }

        val from = From().apply {
            v11 = 7
            v12 = 8L
        }

        val res = mapper.transform(from)
        println(gson.toJson(res))

        assertEquals(14, res.v11)
        assertEquals("8", res.v13)
        assertEquals("x", res.v14)
    }

    @Test
    fun test_mappings_nulls_auto() {

        class From(
            val v01: Int? = null,
            val v02: Long? = null
        ) {
            var v11: Int? = null
            var v12: Long? = null
        }

        class To {
            var v01: Long = 5L
            var v02: String = "x"
            var v11: Long = 5L
            var v12: String = "x"
        }

        val mapper = LaMapper.autoMapper<From, To>()

        val from = From()

        val res = mapper.transform(from)
        println(gson.toJson(res))

        assertEquals(0, res.v01)
        assertEquals("", res.v02)
        assertEquals(0, res.v11)
        assertEquals("", res.v12)
    }

    @Test
    fun test_mappings_nulls_manual() {

        class From(
            var v01: Int? = null,
            var v02: Long? = null
        ) {
            var v11: Int? = null
            var v12: Long? = null
        }

        class To {
            var v01: Long = 5L
            var v02: String = "x"
            var v11: Long = 5L
            var v12: String = "x"
        }

        val mapper = LaMapper.autoMapper<From, To> {
            To::v01 from { null }
            To::v02 from { null }
            To::v11 from { null }
            To::v12 from From::v12
        }

        val from = From()

        val res = mapper.transform(from)
        println(gson.toJson(res))

        assertEquals(0, res.v01)
        assertEquals("", res.v02) // auto-convert to ""
        assertEquals(0, res.v11)
        assertEquals("", res.v12) // auto-convert to ""
    }

    @Test
    fun test_mappings_constructor_args_() {
        // not full argument constructor (will use 'hashMap' mapping)
        class From(
            var v01: Int? = 4,
            var v02: Long? = 4,
            var v03: Long? = 4,
        ) {
            var v11: Int = 4
            var v12: Long? = 4
            var v13: Long? = 4
        }

        class To(
            var v01: Long = 5, // var with mapper
            val v02: String = "x",
            var v03: String = "x", // var, no mapper
            var v11: Long? = 5L, // var nullable
            val v12: String? = "x", // val nullable
            var v13: Int = 5, // var, no mapper
            var v14: Int? = 5, // not existing in from
            var v15: Int = 5, // not existing in from, primitive
        )

        val mapper = LaMapper.autoMapper<From, To> {
            To::v01 from { 3 }
            To::v12 from From::v12
        }

        val from = From()

        val res = mapper.transform(from)
        println(gson.toJson(res))

        assertEquals(3, res.v01)
        assertEquals("4", res.v02)
        assertEquals("4", res.v03)
        assertEquals(4, res.v11)
        assertEquals("4", res.v12)
        assertEquals(4, res.v13)
        assertEquals(5, res.v14)
        assertEquals(5, res.v15)
    }

    class TestPojo {
        var field1: String? = null
        var field2: String? = null
        var prop1: String? = null
        var prop2: String? = null
        var prop3: String? = null
    }

    @Test
    fun test_java_pojo_fields_visibility() {
        val from = Test1Pojo().apply {
            field1 = "x"
            assignField2("x")
            prop2 = "x"
            prop3 = "x"
        }

        val mapper = LaMapper.autoMapper<Test1Pojo, TestPojo>()

        val res = mapper.transform(from)
        assertEquals("x", res.field1)
        assertEquals("x", res.field2)
        assertEquals("prop1", res.prop1)
        assertEquals("x", res.prop2)
        assertEquals("x", res.prop3)

        // convert back
        val mapper2 = LaMapper.autoMapper<TestPojo, Test1Pojo>()
        val res2 = mapper2.transform(res)
        assertEquals("x", res2.field1)
        assertEquals("x", res2.retrieveField2())
        assertEquals("prop1", res2.prop1)
        assertEquals("x", res2.prop2)
        assertEquals("x", res2.prop3)
    }

    @Test
    fun test_java_pojo_no_constructor() {
        val from = TestPojo().apply {
            prop1 = "x"
        }
        assertThrows(IllegalArgumentException::class.java) {
            val mapper = LaMapper.autoMapper<TestPojo, Test2PojoConstr>()
            mapper.transform(from)
        }
    }

    private fun assertEqBigDecimal(expected: BigDecimal, value: BigDecimal) {
        assertTrue(value.compareTo(expected) == 0, "Expected '$expected', got '$value'")
    }

    private fun assertEqBigDecimal(expectedAsStr: String, value: BigDecimal) {
        assertTrue(value.compareTo(BigDecimal(expectedAsStr)) == 0, "Expected '$expectedAsStr', got '$value'")
    }
}
