package performance

import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.AutoMapperImpl
import com.github.labai.utils.mapper.MapperCompiler
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/*
 * @author Augustus
 * created on 2023-01-22
 *
 * with few transformations
 *
 * pojo=1699, comp=371, asgn=614, mapr=2271
 *
*/
class TestPerformance2 {

    @Test
    @Disabled
    fun test_performance_1() {
        PerfHelper.testForClasses(
            createFromFn = { createFrom(it) },
            createToFn = { fr -> To(fr.aaa.toInt(), fr.fld2, fr.fld3, fr.fld4) },
            mapperFn = { fr -> fr.toTo() },
            assignFn = { fr -> To.copyFromFrom(fr) },
            compiledFn = { fr -> compiledMapper.transform(fr) }
        )
    }

    private val mapper: AutoMapper<From, To> = LaMapper.autoMapper() {
        To::arg2 from { 2 }
        To::arg3 from { 3 }
        To::arg4 from From::fld4
        To::ddd from From::ccc
        To::eee from { "ccc-${it.ccc}" }
    }
    private val compiledMapper = MapperCompiler(LaMapper.global).compiledMapper(mapper as AutoMapperImpl)!!

    class From {
        var aaa = "123"
        var bbb = "bbb"
        val ccc = 1

        val fld2: String = "fld2"
        val fld3: Int? = null
        val fld4: Int = 4

        var a00 = "00"
        var a01 = "01"
        var a02 = "02"
        var a03 = "03"
        var a04 = "04"
        var a05 = "05"
        var a06 = "06"
        var a07 = "07"
        var a08 = "08"
        var a09 = "09"
        var a10 = "10"
        var a11 = "11"
        var a12 = "12"
        var a13 = "13"
        var a14 = "14"
        var a15 = "15"
        var a16 = "16"
        var a17: String? = null
        var a18: String? = null
        var a19: String? = null

    }

    class To(
        val aaa: Int,
        val arg2: String,
        val arg3: Int? = null,
        val arg4: Int = 5,
    ) {
        var bbb: String? = null
        var ddd: String? = null
        var eee: String? = null

        var a00: Short? = null
        var a01: Int? = null
        var a02: Long? = null
        var a03: BigDecimal? = null
        var a04: String? = null
        var a05: String? = null
        var a06: String? = null
        var a07: String? = null
        var a08: String? = null
        var a09: String? = null
        var a10 = "x10"
        var a11 = "x11"
        var a12 = "x12"
        var a13 = "x13"
        var a14 = "x14"
        var a15 = "x15"
        var a16 = "x16"
        var a17 = "x17"
        var a18 = "x18"
        var a19 = "x19"

        companion object {
            fun copyFromFrom(fr: From) = To(
                aaa = fr.aaa.toInt(),
                arg2 = fr.fld2,
                arg3 = fr.fld3,
                arg4 = fr.fld4,
            ).apply {
                this.bbb = fr.bbb
                this.ddd = fr.ccc.toString()
                this.eee = "ccc-${fr.ccc}"

                this.a00 = fr.a00.toShortOrNull()
                this.a01 = fr.a01.toIntOrNull()
                this.a02 = fr.a02.toLongOrNull()
                this.a03 = fr.a03.toBigDecimalOrNull()
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
                this.a17 = fr.a17 ?: ""
                this.a18 = fr.a18 ?: ""
                this.a19 = fr.a19 ?: ""
            }
        }

        override fun toString(): String {
            return "To(aaa=$aaa, bbb=$bbb, ddd=$ddd, eee=$eee, a00=$a00, a01=$a01, a02=$a02, a03=$a03, a04=$a04, a05=$a05, a06=$a06, a07=$a07, a08=$a08, a09=$a09, a10='$a10', a11='$a11', a12='$a12', a13='$a13', a14='$a14', a15='$a15', a16='$a16', a17='$a17', a18='$a18', a19='$a19')"
        }
    }

    private fun createFrom(i: Int): From {
        return From().apply {
            a00 = "$i"
            a01 = "$i"
            a02 = "$i"
            a03 = "$i"
            a04 = "$i"
            a05 = "$i"
            a06 = "$i"
            a07 = "$i"
            a08 = "$i"
            a09 = "$i"
            a10 = "a10-$i"
            a11 = "a11-$i"
            a12 = "a12-$i"
            a13 = "a13-$i"
            a14 = "a14-$i"
            a15 = "a15-$i"
            a16 = "a16-$i"
            a17 = null
            a18 = null
            a19 = null
        }
    }

    private fun From.toTo() = mapper.transform(this)

}
