import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.AutoMapperImpl
import com.github.labai.utils.mapper.MapperCompiler
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperValueClassTest {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun <Fr : Any, To : Any> getMapper(type: String, mapper: AutoMapper<Fr, To>): AutoMapper<Fr, To> {
        if (type == "reflect")
            return mapper
        else if (type == "compile") {
            return MapperCompiler(LaMapper.global).compiledMapper(mapper as AutoMapperImpl)!!
        }
        throw IllegalArgumentException("Invalid mapper type")
    }

    // ----------------------------------------------------------------

    @JvmInline
    value class Age(val value: Int) {
        override fun toString() = "Age($value)"
    }

    @JvmInline
    value class AgeS(val age: String) {
        override fun toString() = "AgeS($age)"
    }

    @JvmInline
    value class AgeX(val age: Long) {
        override fun toString() = "AgeX($age)"
    }

    class Fr091(val age: Age)
    class To091(val age: Long)

    @ParameterizedTest
    @ValueSource(strings = ["reflect", "compile"])
    fun test09_1_mappings_value_class_value_to_primitive_arg(type: String) {

        val from = Fr091(Age(10))
        val mapperx = LaMapper.autoMapper<Fr091, To091>()
        val mapper = getMapper(type, mapperx)

        val res = mapper.transform(from)
        assertEquals(10, res.age)

        val mapper2x = LaMapper.autoMapper<To091, Fr091>()
        val mapper2 = getMapper(type, mapper2x)

        val res2 = mapper2.transform(res)
        assertEquals(Age(10), res2.age)
    }

    class Fr092 {
        var age: Age? = null
    }

    class To092 {
        var age: Long? = null
    }

    @ParameterizedTest
    @ValueSource(strings = ["reflect", "compile"])
    fun test09_2_mappings_value_class_value_to_primitive_prop(type: String) {

        val from = Fr092().apply { age = Age(10) }
        val mapperx = LaMapper.autoMapper<Fr092, To092>()
        val mapper = getMapper(type, mapperx)
        val res = mapper.transform(from)
        assertEquals(10, res.age)

        val mapper2x = LaMapper.autoMapper<To092, Fr092>()
        val mapper2 = getMapper(type, mapper2x)
        val res2 = mapper2.transform(res)
        assertEquals(Age(10), res2.age)
    }

    class Fr093(val age: Age)
    class To093(val age: AgeS)

    @ParameterizedTest
    @ValueSource(strings = ["reflect", "compile"])
    fun test09_3_mappings_value_class_value_to_value_string(type: String) {

        val from = Fr093(age = Age(10))
        val mapperx = LaMapper.autoMapper<Fr093, To093>()
        val mapper = getMapper(type, mapperx)
        val res = mapper.transform(from)
        assertEquals(AgeS("10"), res.age)

        val mapper2x = LaMapper.autoMapper<To093, Fr093>()
        val mapper2 = getMapper(type, mapper2x)
        val res2 = mapper2.transform(res)
        assertEquals(Age(10), res2.age)
    }

    class Fr094(val age: Age)
    class To094(val age: AgeX)

    @ParameterizedTest
    @ValueSource(strings = ["reflect", "compile"])
    fun test09_4_mappings_value_class_value_to_value(type: String) {
        val from = Fr094(age = Age(10))

        val mapperx = LaMapper.autoMapper<Fr094, To094>()
        val mapper = getMapper(type, mapperx)
        val res = mapper.transform(from)
        assertEquals(AgeX(10), res.age)

        val mapper2x = LaMapper.autoMapper<To094, Fr094>()
        val mapper2 = getMapper(type, mapper2x)
        val res2 = mapper2.transform(res)
        assertEquals(Age(10), res2.age)
    }

    @JvmInline
    value class AgeUInt(val value: UInt) {
        override fun toString() = "AgeUInt($value)"
    }

    @JvmInline
    value class AgeUByte(val age: UByte) {
        override fun toString() = "AgeUByte($age)"
    }

    class Fr095(val age: AgeUInt)
    class To095(val age: AgeUByte)

    @ParameterizedTest
    @ValueSource(strings = ["reflect", "compile"])
    fun test09_5_mappings_value_class_unumber(type: String) {
        val from = Fr095(age = AgeUInt(10u))
        val mapperx = LaMapper.autoMapper<Fr095, To095>()
        val mapper = getMapper(type, mapperx)
        val res = mapper.transform(from)
        assertEquals(AgeUByte(10u), res.age)

        val mapper2x = LaMapper.autoMapper<To095, Fr095>()
        val mapper2 = getMapper(type, mapper2x)
        val res2 = mapper2.transform(res)
        assertEquals(AgeUInt(10u), res2.age)
    }
}
