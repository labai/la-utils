package com.github.labai.deci.converter

import com.github.labai.deci.Deci
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Test
import com.github.labai.deci.converter.gson.GsonDeciRegister
import kotlin.test.assertEquals

/**
 * @author Augustus
 * created on 2020.11.29
 */
class GsonDeciTest {
    val gson: Gson
    init {
        gson = GsonBuilder()
            .registerTypeAdapter(Deci::class.java, GsonDeciRegister.deciTypeAdapter())
            .create();
    }

    class Data1 {
        var deci: Deci? = null
        var str: String? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Data1
            if (deci != other.deci) return false
            if (str != other.str) return false
            return true
        }

        override fun hashCode(): Int {
            var result = deci?.hashCode() ?: 0
            result = 31 * result + (str?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Data1(deci=$deci, str=$str)"
        }

    }


    @Test
    fun test_gson() {
        val d = Data1()
        d.deci = Deci("-12.345")
        d.str = "abra"
        val json = gson.toJson(d)
        println(json)
        assertEquals("""{"deci":-12.345,"str":"abra"}""", json)
        val d2 = gson.fromJson<Data1>(json, Data1::class.java)
        println(d2)
        assertEquals("Data1(deci=-12.345, str=abra)", d2.toString())


    }
}
