package performance

import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.LaMapperImpl.AutoMapperImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * @author Augustus
 *         created on 2022.11.16
 *
 * 20 fields, no transformation, simple fields assign
 *
 * Results
 *  pojo - java reflection pojo copy
 *  comp - script-compiled mapper
 *  asgn - hardcoded assign
 *  mapr - default mapper
 *  refl - reflection mapper (disabled precompile)
 *
 * kotlin 1.7.22, jvm 8
 *  1. Copy properties
 *  j8  pojo=986, comp=27, asgn=29, mapr=245, refl=1355
 *  j11 pojo=1211, comp=48, asgn=84, mapr=427, refl=2744
 *  j17 pojo=1281, comp=43, asgn=91, mapr=438, refl=2843
 *
 *  2. With map constructor (with optional arguments)
 *  j8  pojo=1007, comp=24, asgn=29, mapr=415, refl=2536
 *  j11 pojo=1261, comp=62, asgn=59, mapr=645, refl=2823
 *  j17 pojo=1282, comp=65, asgn=60, mapr=604, refl=3044
 *
 *  3 With array constructor (when all arguments provided)
 *  j8  pojo=1007, comp=41, asgn=29, mapr=387, refl=813
 *  j11 pojo=1219, comp=74, asgn=89, mapr=576, refl=1100
 *  j17 pojo=1337, comp=69, asgn=80, mapr=607, refl=1014
 *
 */
@Suppress("unused")
@Disabled
class TestPerformance1 {
    private val reflectionLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(partiallyCompile = false, disableSyntheticConstructorCall = true))

    @BeforeEach
    internal fun setUp() {
        // System.gc()
    }

    @Test
    internal fun test_1_performance_with_properties() {
        val mapper: AutoMapper<From, To1Prop> = LaMapper.autoMapper()
        val reflectionMapper: AutoMapper<From, To1Prop> = reflectionLaMapper.autoMapper()
        val compiledMapper = LaMapper.global.laMapperImpl.laMapperScriptCompiler.compiledMapper(mapper as AutoMapperImpl)!!

        println("Start test 1")
        PerfHelper.testForClasses(
            createFromFn = { createFrom(1) },
            createToFn = { To1Prop() },
            mapperFn = { fr -> mapper.transform(fr) },
            assignFn = { fr -> To1Prop.copyFromFrom(fr) },
            compiledFn = { fr -> compiledMapper.transform(fr) },
            reflectionFn = { fr -> reflectionMapper.transform(fr) },
        )
    }

    @Test
    internal fun test_2_performance_with_constructor_map() {
        val mapper: AutoMapper<From, To2CMap> = LaMapper.autoMapper()
        val reflectionMapper: AutoMapper<From, To2CMap> = reflectionLaMapper.autoMapper()
        val compiledMapper = LaMapper.global.laMapperImpl.laMapperScriptCompiler.compiledMapper(mapper as AutoMapperImpl)!!

        println("Start test 2")
        PerfHelper.testForClasses(
            createFromFn = { createFrom(1) },
            createToFn = { To2CMap() },
            mapperFn = { fr -> mapper.transform(fr) },
            assignFn = { fr -> To2CMap.copyFromFrom(fr) },
            compiledFn = { fr -> compiledMapper.transform(fr) },
            reflectionFn = { fr -> reflectionMapper.transform(fr) },
        )
    }

    @Test
    internal fun test_3_performance_with_constructor_array() {
        val mapper: AutoMapper<From, To3CArr> = LaMapper.autoMapper()
        val reflectionMapper: AutoMapper<From, To3CArr> = reflectionLaMapper.autoMapper()
        val compiledMapper = LaMapper.global.laMapperImpl.laMapperScriptCompiler.compiledMapper(mapper as AutoMapperImpl)!!

        println("Start test 3")
        PerfHelper.testForClasses(
            createFromFn = { createFrom(1) },
            createToFn = { To3CArr() },
            mapperFn = { fr -> mapper.transform(fr) },
            assignFn = { fr -> To3CArr.copyFromFrom(fr) },
            compiledFn = { fr -> compiledMapper.transform(fr) },
            reflectionFn = { fr -> reflectionMapper.transform(fr) },
        )
    }

    internal fun createFrom(i: Int): From {
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
    class To2CMap(
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
    class To3CArr(
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
