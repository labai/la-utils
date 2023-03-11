import com.github.labai.utils.mapper.AutoMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import kotlin.reflect.KProperty1

/**
 * @author Augustus
 *         created on 2022.11.16
 *
 *  a big pojo...
 */
@Suppress("unused")
class ComplexPojoTest {

    @ParameterizedTest
    @MethodSource(MappersConfig.ENGINES)
    fun test_assign(engine: String) {
        val mapper = MappersConfig.getMapper<FrX, ToX>(engine) {
            ToX::f02i from FrX::f01i
            ToX::f02in from FrX::f01in
            ToX::f02s from FrX::f01s
            ToX::f02sn from FrX::f01sn
            ToX::f02d from  FrX::f01d

            ToX::f04i from { it.f01i + 1 }
            ToX::f04in from { it.f01in?.plus(1) }
            ToX::f04s from { it.f01s + "x" }
            ToX::f04sn from { it.f01sn?.plus("x") }
            ToX::f04d from { it.f01d.add(1.toBigDecimal()) }

            ToX::f08i from FrX::f01i
            ToX::f08in from FrX::f01in
            ToX::f08s from FrX::f01s
            ToX::f08sn from FrX::f01sn
            ToX::f08d from  FrX::f01d

            ToX::f09i from { it.f01i + 1 }
            ToX::f09in from { it.f01in?.plus(1) }
            ToX::f09s from { it.f01s + "x" }
            ToX::f09sn from { it.f01sn?.plus("x") }
            ToX::f09d from { it.f01d.add(1.toBigDecimal()) }

            ToX::f10l from FrX::f05i
            ToX::f10ln from FrX::f05in
            ToX::f10d from FrX::f05s
            ToX::f10dn from FrX::f05sn
            ToX::f10s from  FrX::f05d

            ToX::f11l from { it.f05i + 1 }
            ToX::f11ln from { it.f05in?.plus(1) }
            ToX::f11d from { it.f05s + "" }
            ToX::f11dn from { it.f05sn?.plus("") }
            ToX::f11s from  { it.f05d.add(1.toBigDecimal()) }

            ToX::f12l from FrX::f05i
            ToX::f12ln from FrX::f05in
            ToX::f12d from FrX::f05s
            ToX::f12dn from FrX::f05sn
            ToX::f12s from  FrX::f05d

            ToX::f13l from { it.f05i + 1 }
            ToX::f13ln from { it.f05in?.plus(1) }
            ToX::f13d from { it.f05s + "" }
            ToX::f13dn from { it.f05sn?.plus("") }
            ToX::f13s from  { it.f05d.add(1.toBigDecimal()) }

            ToX::f15i from FrX::f14in
            ToX::f16i from { it.f14in }

            ToX::f17i from FrX::f14in
            ToX::f18i from { it.f14in }
        }

        test_mapper(mapper)
    }

    private fun test_mapper(mapper: AutoMapper<FrX, ToX>) {
        val frx = FrX()
        val tox = mapper.transform(frx)

        val equalsAssertList = mapOf<KProperty1<ToX, Any?>, Any?>(
            ToX::f01i to 1,
            ToX::f01in to null,
            ToX::f01s to "str",
            ToX::f01sn to null,
            ToX::f01d to 1.toBigDecimal(),

            ToX::f02i  to 1,
            ToX::f02in to null,
            ToX::f02s  to "str",
            ToX::f02sn to null,
            ToX::f02d  to 1.toBigDecimal(),

            // test with defaults
            ToX::f03i  to 1,
            ToX::f03in to null,
            ToX::f03s  to "str",
            ToX::f03sn to null,
            ToX::f03d  to 1.toBigDecimal(),

            // manual lambda
            ToX::f04i  to 2,
            ToX::f04in to null,
            ToX::f04s  to "strx",
            ToX::f04sn to null,
            ToX::f04d  to 2.toBigDecimal(),

            // test with conversions
            ToX::f05i  to 1L,
            ToX::f05in to null,
            ToX::f05s  to "1.1".toBigDecimal(),
            ToX::f05sn to null,
            ToX::f05d  to "1",

            // test properties
            ToX::f06i  to 1,
            ToX::f06in to null,
            ToX::f06s  to "str",
            ToX::f06sn to null,
            ToX::f06d  to 1.toBigDecimal(),

            // test properties with conversions
            ToX::f07i  to 1L,
            ToX::f07in to null,
            ToX::f07s  to "1.1".toBigDecimal(),
            ToX::f07sn to null,
            ToX::f07d  to "1",

            // manual map
            ToX::f08i  to 1,
            ToX::f08in to null,
            ToX::f08s  to "str",
            ToX::f08sn to null,
            ToX::f08d  to 1.toBigDecimal(),

            // map with lambda
            ToX::f09i  to 2,
            ToX::f09in to null,
            ToX::f09s  to "strx",
            ToX::f09sn to null,
            ToX::f09d  to 2.toBigDecimal(),

            // manual map with conversion
            ToX::f10l  to 1L,
            ToX::f10ln to null,
            ToX::f10d  to "1.1".toBigDecimal(),
            ToX::f10dn to null,
            ToX::f10s  to "1",

            // map with lambda with conversion
            ToX::f11l  to 2L,
            ToX::f11ln to null,
            ToX::f11d  to "1.1".toBigDecimal(),
            ToX::f11dn to null,
            ToX::f11s  to "2",

            ToX::f14in to 0,
            ToX::f17i  to 0,  // manual map
            ToX::f18i  to 0,  // manual map
            ToX::f19in to 0,  // auto map null to not null
        )

        equalsAssertList.onEach { assertEquals(it.value, it.key.get(tox), "Not equal ${it.key.name} - got ${it.key.get(tox)}, expected ${it.value}") }

        // w/o defaults
        assertEquals(1, tox.f01i)
        assertEquals(null, tox.f01in)
        assertEquals("str", tox.f01s)
        assertEquals(null, tox.f01sn)
        assertEqBigDecimal("1", tox.f01d)
    }

    // cover all cases
    class FrX() {
        // test arg w/o defaults
        var f01i: Int        = 1
        var f01in: Int?      = null
        var f01s: String     = "str"
        var f01sn: String?   = null
        var f01d: BigDecimal = 1.toBigDecimal()

        // test arg manual
        val f02i: Int        = 1
        val f02in: Int?      = null
        val f02s: String     = "str"
        val f02sn: String?   = null
        val f02d: BigDecimal = 1.toBigDecimal()

        // test with defaults
        val f03i: Int        = 1
        val f03in: Int?      = null
        val f03s: String     = "str"
        val f03sn: String?   = null
        val f03d: BigDecimal = 1.toBigDecimal()

        // test with conversions
        val f05i: Int        = 1
        val f05in: Int?      = null
        val f05s: String     = "1.1"
        val f05sn: String?   = null
        val f05d: BigDecimal = 1.toBigDecimal()

        // test properties
        var f06i: Int        = 1
        var f06in: Int?      = null
        var f06s: String     = "str"
        var f06sn: String?   = null
        var f06d: BigDecimal = 1.toBigDecimal()

        // test properties with conversions
        var f07i: Int        = 1
        var f07in: Int?      = null
        var f07s: String     = "1.1"
        var f07sn: String?   = null
        var f07d: BigDecimal = 1.toBigDecimal()

        val f14in: Int?      = null
        val f19in: Int?      = null
    }

    class ToX(
        // w/o defaults
        val f01i: Int,
        val f01in: Int?,
        val f01s: String,
        val f01sn: String?,
        val f01d: BigDecimal,

        // manual map
        val f02i: Int,
        val f02in: Int?,
        val f02s: String,
        val f02sn: String?,
        val f02d: BigDecimal,

        // with defaults
        val f03i: Int = 1,
        val f03in: Int? = null,
        val f03sn: String? = null,
        val f03s: String = "str",
        val f03d: BigDecimal = 1.toBigDecimal(),

        // manual lambda
        val f04i: Int,
        val f04in: Int?,
        val f04s: String,
        val f04sn: String?,
        val f04d: BigDecimal,

        // with conversions
        val f05i: Long,
        val f05in: Long?,
        val f05s: BigDecimal,
        val f05sn: BigDecimal?,
        val f05d: String,

        // manual map with conversion
        var f12l: Long,
        var f12ln: Long?,
        var f12d: BigDecimal,
        var f12dn: BigDecimal?,
        var f12s: String,

        // map with lambda with conversion
        var f13l: Long,
        var f13ln: Long?,
        var f13d: BigDecimal,
        var f13dn: BigDecimal?,
        var f13s: String,

        val f14in: Int, // notnull from null
        val f15i: Int, // notnull from null manual
        val f16i: Int, // notnull from null manual
    ) {

        var f06i: Int = 1
        var f06in: Int? = null
        var f06s: String = "str"
        var f06sn: String? = null
        var f06d: BigDecimal = 1.toBigDecimal()

        // with conversions
        var f07i: Long = 1
        var f07in: Long? = null
        var f07s: BigDecimal = 1.toBigDecimal()
        var f07sn: BigDecimal? = null
        var f07d: String = "t"

        // manual map
        var f08i: Int = 1
        var f08in: Int? = null
        var f08s: String = "t"
        var f08sn: String? = null
        var f08d: BigDecimal = 1.toBigDecimal()

        // map with lambda
        var f09i: Int = 1
        var f09in: Int? = null
        var f09s: String = "t"
        var f09sn: String? = null
        var f09d: BigDecimal = 1.toBigDecimal()

        // manual map with conversion
        var f10l: Long = 1
        var f10ln: Long? = null
        var f10d: BigDecimal = 1.toBigDecimal()
        var f10dn: BigDecimal? = null
        var f10s: String = ""

        // map with lambda with conversion
        var f11l: Long = 1
        var f11ln: Long? = null
        var f11d: BigDecimal = 1.toBigDecimal()
        var f11dn: BigDecimal? = null
        var f11s: String = ""

        var f17i: Int = 1 // manual map
        var f18i: Int = 1 // manual map
        var f19in: Int = 1 // auto map null to not null
    }

    private fun assertEqBigDecimal(expectedAsStr: String, value: BigDecimal) {
        assertTrue(value.compareTo(BigDecimal(expectedAsStr)) == 0, "Expected '$expectedAsStr', got '$value'")
    }
}
