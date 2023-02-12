package performance

import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.AutoMapperImpl
import com.github.labai.utils.mapper.MapperCompiler
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * @author Augustus
 *         created on 2022.11.16
 *
 * 20 fields, no transformation, simple fields assign
 *
 * Results (kotlin 1.7.20)
 *  asgn - hardcoded assign
 *  comp - compiled mapper
 *  mapr - reflection mapper
 *  pojo - java reflection pojo copy
 *
 *  1. Copy properties
 *  pojo=1024, comp=24, asgn=25, mapr=1518
 *
 *  2. With map constructor (with optional arguments)
 *  pojo=1162, comp=37, asgn=54, mapr=2447
 *
 *  3 With array constructor (when all arguments provided)
 *  pojo=1117, comp=64, asgn=31, mapr=870
 *
 *
 */
@Disabled
class TestPerformance1 {

    @Test
    internal fun test_1_performance_with_properties() {
        val mapper: AutoMapper<From, To1Prop> = LaMapper.autoMapper()
        val compiledMapper = MapperCompiler(LaMapper.global).compiledMapper(mapper as AutoMapperImpl)!!

        PerfHelper.testForClasses(
            createFromFn = { createFrom(it) },
            createToFn = { To1Prop() },
            mapperFn = { fr -> mapper.transform(fr) },
            assignFn = { fr -> To1Prop.copyFromFrom(fr) },
            compiledFn = { fr -> compiledMapper.transform(fr) }
        )
    }

    @Test
    internal fun test_2_performance_with_constructor_map() {
        val mapper: AutoMapper<From, To2CMap> = LaMapper.autoMapper()
        val compiledMapper = MapperCompiler(LaMapper.global).compiledMapper(mapper as AutoMapperImpl)!!

        PerfHelper.testForClasses(
            createFromFn = { createFrom(it) },
            createToFn = { To2CMap() },
            mapperFn = { fr -> mapper.transform(fr) },
            assignFn = { fr -> To2CMap.copyFromFrom(fr) },
            compiledFn = { fr -> compiledMapper.transform(fr) }
        )
    }


    @Test
    internal fun test_3_performance_with_constructor_array() {
        val mapper: AutoMapper<From, To3CArr> = LaMapper.autoMapper()
        val compiledMapper = MapperCompiler(LaMapper.global).compiledMapper(mapper as AutoMapperImpl)!!

        PerfHelper.testForClasses(
            createFromFn = { createFrom(it) },
            createToFn = { To3CArr() },
            mapperFn = { fr -> mapper.transform(fr) },
            assignFn = { fr -> To3CArr.copyFromFrom(fr) },
            compiledFn = { fr -> compiledMapper.transform(fr) }
        )
    }

    private fun createFrom(i: Int): From {
        return From().apply {
            a00 = null
            a01 = "a01-$i"
            a02 = "a02-$i"
            a03 = "a03-$i"
            a04 = "a04-$i"
            a05 = "a05-$i"
            a06 = "a06-$i"
            a07 = "a07-$i"
            a08 = "a08-$i"
            a09 = "a09-$i"
            a10 = "a10-$i"
            a11 = "a11-$i"
            a12 = "a12-$i"
            a13 = "a13-$i"
            a14 = "a14-$i"
            a15 = "a15-$i"
            a16 = "a16-$i"
            a17 = "a17-$i"
            a18 = "a18-$i"
            a19 = "a19-$i"
        }

    }

    class From {
        var a00: String? = null
        var a01: String? = null
        var a02: String? = null
        var a03: String? = null
        var a04: String? = null
        var a05: String? = null
        var a06: String? = null
        var a07: String? = null
        var a08: String? = null
        var a09: String? = null
        var a10: String? = null
        var a11: String? = null
        var a12: String? = null
        var a13: String? = null
        var a14: String? = null
        var a15: String? = null
        var a16: String? = null
        var a17: String? = null
        var a18: String? = null
        var a19: String? = null
    }

    // for properties assign test
    class To1Prop {
        var a00: String? = null
        var a01: String? = null
        var a02: String? = null
        var a03: String? = null
        var a04: String? = null
        var a05: String? = null
        var a06: String? = null
        var a07: String? = null
        var a08: String? = null
        var a09: String? = null
        var a10: String? = null
        var a11: String? = null
        var a12: String? = null
        var a13: String? = null
        var a14: String? = null
        var a15: String? = null
        var a16: String? = null
        var a17: String? = null
        var a18: String? = null
        var a19: String? = null

        companion object {
            fun copyFromFrom(fr: From) = To1Prop().apply {
                this.a00 = fr.a00
                this.a01 = fr.a01
                this.a02 = fr.a02
                this.a03 = fr.a03
                this.a04 = fr.a04
                this.a05 = fr.a05
                this.a06 = fr.a06
                this.a07 = fr.a07
                this.a08 = fr.a08
                this.a09 = fr.a09
                this.a10 = fr.a10
                this.a11 = fr.a11
                this.a12 = fr.a12
                this.a13 = fr.a13
                this.a14 = fr.a14
                this.a15 = fr.a15
                this.a16 = fr.a16
                this.a17 = fr.a17
                this.a18 = fr.a18
                this.a19 = fr.a19
            }
        }
    }

    // for constructor with map parameters test
    class To2CMap (
        var a00: String? = null,
        var a01: String? = null,
        var a02: String? = null,
        var a03: String? = null,
        var a04: String? = null,
        var a05: String? = null,
        var a06: String? = null,
        var a07: String? = null,
        var a08: String? = null,
        var a09: String? = null,
        var a10: String? = null,
        var a11: String? = null,
        var a12: String? = null,
        var a13: String? = null,
        var a14: String? = null,
        var a15: String? = null,
        var a16: String? = null,
        var a17: String? = null,
        var a18: String? = null,
        var x19: String? = null,
    ) {
        companion object {
            fun copyFromFrom(fr: From) = To2CMap(
                a00 = fr.a00,
                a01 = fr.a01,
                a02 = fr.a02,
                a03 = fr.a03,
                a04 = fr.a04,
                a05 = fr.a05,
                a06 = fr.a06,
                a07 = fr.a07,
                a08 = fr.a08,
                a09 = fr.a09,
                a10 = fr.a10,
                a11 = fr.a11,
                a12 = fr.a12,
                a13 = fr.a13,
                a14 = fr.a14,
                a15 = fr.a15,
                a16 = fr.a16,
                a17 = fr.a17,
                a18 = fr.a18,
                x19 = fr.a19,
            )
        }
    }

    // for constructor with array parameters test
    class To3CArr (
        var a00: String? = null,
        var a01: String? = null,
        var a02: String? = null,
        var a03: String? = null,
        var a04: String? = null,
        var a05: String? = null,
        var a06: String? = null,
        var a07: String? = null,
        var a08: String? = null,
        var a09: String? = null,
        var a10: String? = null,
        var a11: String? = null,
        var a12: String? = null,
        var a13: String? = null,
        var a14: String? = null,
        var a15: String? = null,
        var a16: String? = null,
        var a17: String? = null,
        var a18: String? = null,
        var a19: String? = null, // one field do not match - 'map' parameters should be used
    ) {
        companion object {
            fun copyFromFrom(fr: From) = To3CArr(
                a00 = fr.a00,
                a01 = fr.a01,
                a02 = fr.a02,
                a03 = fr.a03,
                a04 = fr.a04,
                a05 = fr.a05,
                a06 = fr.a06,
                a07 = fr.a07,
                a08 = fr.a08,
                a09 = fr.a09,
                a10 = fr.a10,
                a11 = fr.a11,
                a12 = fr.a12,
                a13 = fr.a13,
                a14 = fr.a14,
                a15 = fr.a15,
                a16 = fr.a16,
                a17 = fr.a17,
                a18 = fr.a18,
                a19 = fr.a19,
            )
        }
    }
}
