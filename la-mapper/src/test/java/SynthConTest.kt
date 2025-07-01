import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.DataConverters
import com.github.labai.utils.mapper.impl.LaMapperAsmCompiler3
import com.github.labai.utils.mapper.impl.MappedStructFactory
import com.github.labai.utils.mapper.impl.ServiceContext
import com.github.labai.utils.mapper.impl.SynthConstructorUtils
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

/*
 * @author Augustus
 * created on 2023-02-24
*/

class SynthConTest {
    private val laMapperAsmCompiler: LaMapperAsmCompiler3
    private val serviceContext: ServiceContext
    private val structFactory: MappedStructFactory

    init {
        val config = LaMapperConfig()
        val dataConverters = DataConverters(LaConverterRegistry.global, config)
        serviceContext = ServiceContext().apply { this.config = config; this.dataConverters = dataConverters }
        structFactory = MappedStructFactory(serviceContext)
        laMapperAsmCompiler = LaMapperAsmCompiler3(serviceContext)
    }

    class Pojo1
    class Pojo2(val s: String)
    class Pojo3(val s: String = "")

    @Test
    internal fun test_synth_constructors_in_pojo() {
        // no - w/o optional params
        assertNull(SynthConstructorUtils.findSynthConstructor(Pojo1::class))
        assertNull(SynthConstructorUtils.findSynthConstructor(Pojo2::class))
        // yes
        assertNotNull(SynthConstructorUtils.findSynthConstructor(Pojo3::class))
    }

    @Test
    internal fun test_flagparam_when_dont_exist_should_be_1() {
        val struct = structFactory.createMappedStruct(Pojo1::class, Pojo3::class)

        val synthConConf = SynthConstructorUtils.prepareSynthConParams(struct.targetType, struct.paramBinds)
            ?: fail("can prepare synth constructor call")

        val expected: Array<Any?> = arrayOf(null, 1, null) // 1 - use default (doesn't exists in source)
        assertArrayEquals(expected, synthConConf.synthArgsTemplate)
    }

    @Test
    internal fun test_flagparam_when_exists_should_be_0() {
        val struct = structFactory.createMappedStruct(Pojo2::class, Pojo3::class)

        val synthConConf = SynthConstructorUtils.prepareSynthConParams(struct.targetType, struct.paramBinds)
            ?: fail("can prepare synth constructor call")

        val expected: Array<Any?> = arrayOf(null, 0, null) // 0 - use provided from source class
        assertArrayEquals(expected, synthConConf.synthArgsTemplate)
    }

    class Pojo4(
        val s1: String? = null,
        val s: String,
    )

    @Test
    internal fun test_fail_if_no_source_and_mandatory() {
        try {
            structFactory.createMappedStruct(Pojo1::class, Pojo4::class)
            fail { "expected exception" }
        } catch (e: IllegalArgumentException) {
            // ok
        }
    }

    class Pojo5(
        val s: String,
        val s1: String? = null,
    )

    @Test
    internal fun test_flagparam_when_exists_should_be_1_2nd_param() {
        val struct = structFactory.createMappedStruct(Pojo2::class, Pojo5::class)

        val synthConConf = SynthConstructorUtils.prepareSynthConParams(struct.targetType, struct.paramBinds)
            ?: fail("can prepare synth constructor call")

        val expected: Array<Any?> = arrayOf(null, null, 2, null) // 2 = ..0010 - 2nd param
        assertArrayEquals(expected, synthConConf.synthArgsTemplate)
    }

    @Test
    internal fun test_flagparam_when_exists_should_be_1_32nd_param() {
        val struct = structFactory.createMappedStruct(Pojo2::class, PojoF32::class)

        val synthConConf = SynthConstructorUtils.prepareSynthConParams(struct.targetType, struct.paramBinds)
            ?: fail("can prepare synth constructor call")

        val expected: Array<Any?> = arrayOf(
            0, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, UInt.MAX_VALUE.toInt(), null,
        ) // 32bits: 11...11 - all 32 are optional here and not mapped = UInt.MAX_VALUE
        assertArrayEquals(expected, synthConConf.synthArgsTemplate)
    }

    @Test
    internal fun test_flagparam_when_exists_should_be_1_33nd_param() {
        val struct = structFactory.createMappedStruct(Pojo2::class, PojoF33::class)

        val synthConConf = SynthConstructorUtils.prepareSynthConParams(struct.targetType, struct.paramBinds)
            ?: fail("can prepare synth constructor call")

        val expected: Array<Any?> = arrayOf(
            0, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, UInt.MAX_VALUE.toInt(), 1, null,
        )
        assertArrayEquals(expected, synthConConf.synthArgsTemplate)
    }

    class PojoF32(
        val a01: Int = 0,
        val a02: String = "",
        val a03: String = "",
        val a04: String = "",
        val a05: String = "",
        val a06: String = "",
        val a07: String = "",
        val a08: String = "",
        val a09: String = "",
        val a10: String = "",
        val a11: String = "",
        val a12: String = "",
        val a13: String = "",
        val a14: String = "",
        val a15: String = "",
        val a16: String = "",
        val a17: String = "",
        val a18: String = "",
        val a19: String = "",
        val a20: String = "",
        val a21: String = "",
        val a22: String = "",
        val a23: String = "",
        val a24: String = "",
        val a25: String = "",
        val a26: String = "",
        val a27: String = "",
        val a28: String = "",
        val a29: String = "",
        val a30: String = "",
        val a31: String = "",
        val a32: String = "",
    )

    @Test
    internal fun test_synth_constructor_with_32_field_has_1_flagparam() {
        val c = SynthConstructorUtils.findSynthConstructor(PojoF32::class) ?: fail("constructor not found")
        assertEquals(32 + 1 + 1, c.parameterCount)
    }

    class PojoF33(
        val a01: Int = 0,
        val a02: String = "",
        val a03: String = "",
        val a04: String = "",
        val a05: String = "",
        val a06: String = "",
        val a07: String = "",
        val a08: String = "",
        val a09: String = "",
        val a10: String = "",
        val a11: String = "",
        val a12: String = "",
        val a13: String = "",
        val a14: String = "",
        val a15: String = "",
        val a16: String = "",
        val a17: String = "",
        val a18: String = "",
        val a19: String = "",
        val a20: String = "",
        val a21: String = "",
        val a22: String = "",
        val a23: String = "",
        val a24: String = "",
        val a25: String = "",
        val a26: String = "",
        val a27: String = "",
        val a28: String = "",
        val a29: String = "",
        val a30: String = "",
        val a31: String = "",
        val a32: String = "",
        val a33: String = "",
    )

    @Test
    internal fun test_synth_constructor_with_33_field_has_2_flagparams() {
        val c = SynthConstructorUtils.findSynthConstructor(PojoF33::class) ?: fail("constructor not found")
        assertEquals(33 + 2 + 1, c.parameterCount)
    }

    // ktlint-disable parameter-list-wrapping
    class PojoF65(
        val a01: String = "", val a02: String, val a03: String, val a04: String, val a05: String, val a06: String, val a07: String, val a08: String, val a09: String, val a10: String,
        val a11: String? = null, val a12: String, val a13: String, val a14: String, val a15: String, val a16: String, val a17: String, val a18: String, val a19: String, val a20: String,
        val a21: String? = null, val a22: String, val a23: String, val a24: String, val a25: String, val a26: String, val a27: String, val a28: String, val a29: String, val a30: String,
        val a31: String, val a32: String, val a33: String, val a34: String, val a35: String, val a36: String, val a37: String, val a38: String, val a39: String, val a40: String,
        val a41: String, val a42: String, val a43: String, val a44: String, val a45: String, val a46: String, val a47: String, val a48: String, val a49: String, val a50: String,
        val a51: String, val a52: String, val a53: String, val a54: String, val a55: String, val a56: String, val a57: String, val a58: String, val a59: String, val a60: String,
        val a61: String,
        val a62: String,
        val a63: String,
        val a64: String,
        val a65: String,
    )

    @Test
    internal fun test_synth_constructor_with_65_field_has_3_flagparams() {
        val c = SynthConstructorUtils.findSynthConstructor(PojoF65::class) ?: fail("constructor not found")
        assertEquals(65 + 3 + 1, c.parameterCount)
    }
}
