import StructJv.Test1Pojo
import StructJv.Test2PojoConstr
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.AutoMapperImpl
import com.github.labai.utils.mapper.MapperCompiler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.lang.NullPointerException

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperJavaTest {

    private fun <Fr : Any, To : Any> getMapper(type: String, mapper: AutoMapper<Fr, To>): AutoMapper<Fr, To> {
        if (type == "reflect")
            return mapper
        else if (type == "compile") {
            return MapperCompiler(LaMapper.global).compiledMapper(mapper as AutoMapperImpl)!!
        }
        throw IllegalArgumentException("Invalid mapper type")
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
    @ValueSource(strings = ["reflect", "compile"])
    fun test08_1_java_pojo_fields_visibility_from_java(type: String) {
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

        val mapperx = LaMapper.autoMapper<Test1Pojo, TestPojo>()
        val mapper = getMapper(type, mapperx)

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
    @ValueSource(strings = ["reflect", "compile"])
    fun test08_2_java_pojo_fields_visibility_to_java(type: String) {
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

        val mapperx = LaMapper.autoMapper<TestPojo, Test1Pojo>()
        val mapper = getMapper(type, mapperx)

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
    @ValueSource(strings = ["reflect", "compile"])
    fun test08_3_java_pojo_no_constructor(type: String) {
        val from = TestPojo().apply {
            prop1 = "x"
        }
        try {
            val mapperx = LaMapper.autoMapper<TestPojo, Test2PojoConstr>()
            val mapper = getMapper(type, mapperx)
            mapper.transform(from)
            fail { "Expected exception" }
        } catch (e: Exception) {
            if ((type == "reflect" && e is IllegalArgumentException) || (type == "compile" && e is NullPointerException))
                // ok
            else
                fail { "Invalid exception type: $e ${e.message}" }
        }
    }
}
