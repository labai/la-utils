import StructuresInJava.Test1Pojo
import StructuresInJava.Test2PojoConstr
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperJavaTest {

    companion object {
        @JvmStatic
        private fun engines(): Stream<String>? {
            return Stream.of("default", "reflect", "nosynth", "compile")
        }
    }

    // ----------------------------------------------------------------
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
    @MethodSource("engines")
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

        val mapper = getMapper<Test1Pojo, TestPojo>(engine)

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
    @MethodSource("engines")
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

        val mapper = getMapper<TestPojo, Test1Pojo>(engine)

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
    @MethodSource("engines")
    fun test08_3_java_pojo_no_constructor(engine: String) {
        val from = TestPojo().apply {
            prop1 = "x"
        }
        try {
            val mapper = getMapper<TestPojo, Test2PojoConstr>(engine)
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
}
