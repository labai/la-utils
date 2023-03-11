import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperValueClassTest {

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
    @MethodSource(MappersConfig.ENGINES)
    fun test09_1_mappings_value_class_value_to_primitive_arg(engine: String) {
        val from = Fr091(Age(10))
        val mapper = MappersConfig.getMapper<Fr091, To091>(engine)

        val res = mapper.transform(from)
        assertEquals(10, res.age)

        val mapper2 = MappersConfig.getMapper<To091, Fr091>(engine)

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
    @MethodSource(MappersConfig.ENGINES)
    fun test09_2_mappings_value_class_value_to_primitive_prop(engine: String) {
        val from = Fr092().apply { age = Age(10) }
        val mapper = MappersConfig.getMapper<Fr092, To092>(engine)
        val res = mapper.transform(from)
        assertEquals(10, res.age)

        val mapper2 = MappersConfig.getMapper<To092, Fr092>(engine)
        val res2 = mapper2.transform(res)
        assertEquals(Age(10), res2.age)
    }

    class Fr093(val age: Age)
    class To093(val age: AgeS)

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test09_3_mappings_value_class_value_to_value_string(engine: String) {
        val from = Fr093(age = Age(10))
        val mapper = MappersConfig.getMapper<Fr093, To093>(engine)
        val res = mapper.transform(from)
        assertEquals(AgeS("10"), res.age)

        val mapper2 = MappersConfig.getMapper<To093, Fr093>(engine)
        val res2 = mapper2.transform(res)
        assertEquals(Age(10), res2.age)
    }

    class Fr094(val age: Age)
    class To094(val age: AgeX)

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test09_4_mappings_value_class_value_to_value(engine: String) {
        val from = Fr094(age = Age(10))

        val mapper = MappersConfig.getMapper<Fr094, To094>(engine)
        val res = mapper.transform(from)
        assertEquals(AgeX(10), res.age)

        val mapper2 = MappersConfig.getMapper<To094, Fr094>(engine)
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
    @MethodSource(MappersConfig.ENGINES)
    fun test09_5_mappings_value_class_unumber(engine: String) {
        val from = Fr095(age = AgeUInt(10u))
        val mapper = MappersConfig.getMapper<Fr095, To095>(engine)
        val res = mapper.transform(from)
        assertEquals(AgeUByte(10u), res.age)

        val mapper2 = MappersConfig.getMapper<To095, Fr095>(engine)
        val res2 = mapper2.transform(res)
        assertEquals(AgeUInt(10u), res2.age)
    }
}
