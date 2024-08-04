import jtest.StructuresInJava.Record12
import jtest.StructuresInJava.Test1Pojo
import jtest.StructuresInJava.Test2PojoConstr
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperJavaTest {

    class TestPojo {
        var field1: String? = null
        var field2: String? = null
        var prop1: String? = null
        var prop2: String? = null
        var prop3: String? = null
        var prop4: String? = null
        var prop5: String? = null
        var wrong1: String? = null
        var wrong2: Int? = null
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test08_1_java_pojo_fields_visibility_from_java(engine: String) {
        val from = Test1Pojo().apply {
            field1 = "x"
            assignField2("x")
            prop2 = "x"
            prop3 = "x"
            prop4 = "x"
            prop5 = "x"
            wrong1 = "x"
            wrong2 = 1
        }

        val mapper = MappersConfig.getMapper<Test1Pojo, TestPojo>(engine)

        val res = mapper.transform(from)
        assertEquals("x", res.field1) // public field
        assertEquals(null, res.field2) // private field. DON'T read it
        assertEquals("prop1", res.prop1) // getter
        assertEquals("x", res.prop2) // getter and setter
        assertEquals("x", res.prop3) // public field, getter and setter
        assertEquals("x", res.prop4)
        assertEquals("x", res.prop5)
        assertEquals(null, res.wrong1) // field must much getter name in Java
        assertEquals(null, res.wrong2) // field must much getter type in Java
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test08_2_java_pojo_fields_visibility_to_java(engine: String) {
        val from = TestPojo().apply {
            field1 = "x"
            field2 = "x"
            prop2 = "x"
            prop3 = "x"
            prop4 = "x"
            prop5 = "x"
            wrong1 = "x"
            wrong2 = 1
        }

        val mapper = MappersConfig.getMapper<TestPojo, Test1Pojo>(engine)

        val res = mapper.transform(from)
        assertEquals("x", res.field1)
        assertEquals(null, res.retrieveField2()) // private field. DON'T change it
        assertEquals("prop1", res.prop1) // no setter
        assertEquals("x", res.prop2)
        assertEquals("x", res.prop3)
        assertEquals("x", res.prop4)
        assertEquals("x", res.prop5)
        assertEquals(null, res.wrong1)
        assertEquals(null, res.wrong2)
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test08_3_java_pojo_no_constructor(engine: String) {
        val from = TestPojo().apply {
            prop1 = "x"
        }
        try {
            val mapper = MappersConfig.getMapper<TestPojo, Test2PojoConstr>(engine)
            mapper.transform(from)
            fail { "Expected exception" }
        } catch (e: Exception) {
            if (engine == "compile" && e is NullPointerException)
            // ok
            else if (e is IllegalArgumentException)
            // ok
            else
                fail { "Invalid exception type: $e ${e.message}" }
        }
    }

    class Dto12(
        var v01: Long = 1L,
        var v02: Long? = 2L,
        var v03: Int = 3,
        var v04: Int? = 4,
        var v05: String? = "5",
    )

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test12_dto_to_record(engine: String) {
        val mapper = MappersConfig.getMapper<Dto12, Record12>(engine) {
            Dto12::v01 mapTo t::v04
            f::v02 mapTo t::v03
            f::v03 mapTo t::v02
            Record12::v04 mapTo Dto12::v01
            t::v06 from { "x" }
        }

        val from = Dto12()

        val res = mapper.transform(from)

        assertEquals(4, res.v01)
        assertEquals(3, res.v02)
        assertEquals(2, res.v03)
        assertEquals(1, res.v04)
        assertEquals("x", res.v06)
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test12_dto_to_record_missing_arg(engine: String) {
        val mapperMissingV6 = MappersConfig.getMapper<Dto12, Record12>(engine) //
        assertThrows<IllegalArgumentException> {
            mapperMissingV6.transform(Dto12())
        }
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test12_record_to_dto(engine: String) {
        val mapper = MappersConfig.getMapper<Record12, Dto12>(engine) {
            Dto12::v01 from f::v04
            t::v02 from f::v03
            t::v03 from f::v02
            Dto12::v04 from f::v01
        }

        val from = Record12(1, 2, 3, 4, "6")

        val res = mapper.transform(from)

        assertEquals(4, res.v01)
        assertEquals(3, res.v02)
        assertEquals(2, res.v03)
        assertEquals(1, res.v04)
        assertEquals("5", res.v05) // leave default value
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test12_record_to_record(engine: String) {
        val mapper = MappersConfig.getMapper<Record12, Record12>(engine) {
            Record12::v01 from f::v04
            t::v02 from f::v03
            t::v03 from f::v02
            Record12::v04 from f::v01
        }

        val from = Record12(1, 2, 3, 4, "6")

        val res = mapper.transform(from)

        assertEquals(4, res.v01)
        assertEquals(3, res.v02)
        assertEquals(2, res.v03)
        assertEquals(1, res.v04)
        assertEquals("6", res.v06)
    }
}
