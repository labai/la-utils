import com.github.labai.utils.mapper.LaMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @author Augustus
 *         created on 2022.11.16
 */
class LaMapperTest {

    data class Fr01(
        val v1: Int,
        val v2: BigDecimal,
        val `v=3`: String,
    )

    class To01(
        val v1: BigDecimal,
        val v2: BigDecimal,
        val `v=3`: String,
    )

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test01_constructor_to_constructor(engine: String) {
        val mapper = MappersConfig.getMapper<Fr01, To01>(engine)

        val from = Fr01(5, BigDecimal("6.5"), "foo")

        val res = mapper.transform(from)

        assertEqBigDecimal("5", res.v1)
        assertEqBigDecimal("6.5", res.v2)
        assertEquals("foo", res.`v=3`)

        // same with copyFrom()
        val res2: To01 = LaMapper.copyFrom(from)
        assertEqBigDecimal("5", res2.v1)
        assertEqBigDecimal("6.5", res2.v2)
        assertEquals("foo", res2.`v=3`)
    }

    // ----------------------------------------------------------------
    class Fr02 {
        var a20: Int? = null
        var a30: LocalDate? = null
    }

    class To02 {
        var a20: Long? = null
        var a30: LocalDateTime? = null
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test02_fields_assign(engine: String) {
        val mapper = MappersConfig.getMapper<Fr02, To02>(engine)

        val from = Fr02().apply {
            a20 = 7
            a30 = LocalDate.parse("2022-11-08")
        }

        val res = mapper.transform(from)

        assertEquals(7L, res.a20)
        assertEquals(LocalDateTime.parse("2022-11-08T00:00:00"), res.a30)
    }

    // ----------------------------------------------------------------
    class Fr03(val v1: Int, val v2: BigDecimal) {
        var v3: Int? = null
        var v4: LocalDate? = null
        var v5: Int? = null
    }

    class To03(val v1: BigDecimal, var v3: Long? = null, val v5: Int) {
        var v2: BigDecimal? = null
        var v4: LocalDateTime? = null
        val v6: String = "justGetter"
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test03_mixed_assign(engine: String) {
        val mapper = MappersConfig.getMapper<Fr03, To03>(engine)

        val from = Fr03(5, BigDecimal("6.5")).apply {
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

    // ----------------------------------------------------------------
    class Fr04 {
        var v11: Int? = null
        var v12: Long? = null
    }

    class To04 {
        var v11: Long? = null
        var v13: String? = null
        var v14: String = "x"
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test04_mappings_property_mapping(engine: String) {
        val mapper = MappersConfig.getMapper<Fr04, To04>(engine) {
            To04::v11 from { it.v11!! * 2 }
            To04::v13 from Fr04::v12 // should be auto-convert by types as well (Long->String)
        }

        val from = Fr04().apply {
            v11 = 7
            v12 = 8L
        }

        val res = mapper.transform(from)
        // printJson(res)

        assertEquals(14, res.v11)
        assertEquals("8", res.v13)
        assertEquals("x", res.v14)
    }

    // ----------------------------------------------------------------
    class Fr05(
        val v01: Int? = null,
        val v02: Long? = null,
    ) {
        var v11: Int? = null
        var v12: Long? = null
    }

    class To05 {
        var v01: Long = 5L
        var v02: String = "x"
        var v11: Long = 5L
        var v12: String = "x"
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test05_mappings_nulls_auto(engine: String) {
        val mapper = MappersConfig.getMapper<Fr05, To05>(engine)

        val from = Fr05()

        val res = mapper.transform(from)
        // printJson(res)

        assertEquals(0, res.v01)
        assertEquals("", res.v02)
        assertEquals(0, res.v11)
        assertEquals("", res.v12)
    }

    // ----------------------------------------------------------------
    class Fr06(
        var v01: Int? = null,
        var v02: Long? = null,
    ) {
        var v11: Int? = null
        var v12: Long? = null
    }

    class To06 {
        var v01: Long = 5L
        var v02: String = "x"
        var v11: Long = 5L
        var v12: String = "x"
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test06_mappings_nulls_manual(engine: String) {
        val mapper = MappersConfig.getMapper<Fr06, To06>(engine) {
            To06::v01 from { null }
            To06::v02 from { null }
            To06::v11 from { null }
            To06::v12 from Fr06::v12
        }

        val from = Fr06()

        val res = mapper.transform(from)
        // printJson(res)

        assertEquals(0, res.v01)
        assertEquals("", res.v02) // auto-convert to ""
        assertEquals(0, res.v11)
        assertEquals("", res.v12) // auto-convert to ""
    }

    // ----------------------------------------------------------------
    class Fr07(
        var v01: Int? = 4,
        var v02: Long? = 4,
        var v03: Long? = 4,
    ) {
        var v11: Int = 4
        var v12: Long? = 4
        var v13: Long? = 4
    }

    class To08(
        var v01: Long = 5, // var with mapper
        val v02: String = "x",
        var v03: String = "x", // var, no mapper
        var v11: Long? = 5L, // var nullable
        val v12: String? = "x", // val nullable
        var v13: Int = 5, // var, no mapper
        var v14: Int? = 5, // not existing in from
        var v15: Int = 5, // not existing in from, primitive
    )

    // not full argument constructor (will use 'map' mapping or special synth call)
    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test07_mappings_constructor_args(engine: String) {
        val mapper = MappersConfig.getMapper<Fr07, To08>(engine) {
            To08::v01 from { 3 }
            To08::v12 from Fr07::v12
        }

        val from = Fr07()

        val res = mapper.transform(from)
        // printJson(res)

        assertEquals(3, res.v01)
        assertEquals("4", res.v02)
        assertEquals("4", res.v03)
        assertEquals(4, res.v11)
        assertEquals("4", res.v12)
        assertEquals(4, res.v13)
        assertEquals(5, res.v14)
        assertEquals(5, res.v15)
    }

    interface IFr10
    class Fr10(val a01: String) : IFr10
    class To10(val a01: String)

    @Test
    internal fun test10_from_interface_subclass() {
        val fr: IFr10 = Fr10("a")
        val res = LaMapper.global.copyFrom(
            from = fr,
            sourceType = fr.javaClass.kotlin,
            targetType = To10::class,
            mapping = null,
        )
        assertEquals("a", res.a01)
    }

    class Dto11(
        var v01: Long = 1L,
        var v02: Long = 2L,
        var v03: Int = 3,
        var v04: Int = 4,
        var v05: Long = 5L,
        var v06: Long = 6L,
    )

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test11_mapWithPseudoObjectHelper_from(engine: String) {
        val mapper = MappersConfig.getMapper<Dto11, Dto11>(engine) {
            t::v01 from Dto11::v04
            Dto11::v02 from f::v03
            t::v03 from f::v02
            Dto11::v04 from Dto11::v01
            t::v05 from { 105 }
            Dto11::v06 from { 106 }
        }

        val from = Dto11()

        val res = mapper.transform(from)

        assertEquals(4, res.v01)
        assertEquals(3, res.v02)
        assertEquals(2, res.v03)
        assertEquals(1, res.v04)
        assertEquals(105, res.v05)
        assertEquals(106, res.v06)
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test11_mapWithPseudoObjectHelper_mapTo(engine: String) {
        val mapper = MappersConfig.getMapper<Dto11, Dto11>(engine) {
            Dto11::v04 mapTo t::v01
            f::v03 mapTo Dto11::v02
            f::v02 mapTo t::v03
            Dto11::v01 mapTo Dto11::v04
        }

        val from = Dto11()

        val res = mapper.transform(from)

        assertEquals(4, res.v01)
        assertEquals(3, res.v02)
        assertEquals(2, res.v03)
        assertEquals(1, res.v04)
    }

    @JvmRecord
    data class Dto12(
        val v01: Long = 1L,
    )

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test12_record_with_mapper(engine: String) {
        val mapper = MappersConfig.getMapper<Dto12, Dto12>(engine) {
            Dto12::v01 from { 4 }
        }
        val from = Dto12()
        val res = mapper.transform(from)
        assertEquals(4, res.v01)
    }

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test13_hashMap_var(engine: String) {
        // test_1 (data struct)
        val mapper1 = MappersConfig.getMapper<Map<String, Any?>, To01>(engine)
        val from1 = mutableMapOf<String, Any?>("v1" to 5, "v2" to "6.5", "v=3" to "foo")
        val res1 = mapper1.transform(from1)

        assertEqBigDecimal("5", res1.v1)
        assertEqBigDecimal("6.5", res1.v2)
        assertEquals("foo", res1.`v=3`)

        // test_2 (pojo)
        val mapper2 = MappersConfig.getMapper<Map<String, Any?>, To02>(engine)
        val from2 = mutableMapOf<String, Any?>("a20" to 7, "a30" to LocalDate.parse("2022-11-08"))
        val res2 = mapper2.transform(from2)

        assertEquals(7L, res2.a20)
        assertEquals(LocalDateTime.parse("2022-11-08T00:00:00"), res2.a30)

        // test_3 (record)
        val mapper3 = MappersConfig.getMapper<Map<*, *>, Dto12>(engine)
        val from3 = mutableMapOf<String, Any?>("v01" to 5)
        val res3 = mapper3.transform(from3)
        assertEquals(5, res3.v01)
    }

    private fun assertEqBigDecimal(expectedAsStr: String, value: BigDecimal) {
        assertTrue(value.compareTo(BigDecimal(expectedAsStr)) == 0, "Expected '$expectedAsStr', got '$value'")
    }
}
