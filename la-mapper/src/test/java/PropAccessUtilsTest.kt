import com.github.labai.utils.mapper.impl.PropAccessUtils
import jtest.StructuresInJava.Test1Pojo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.reflect.full.createType

/*
 * @author Augustus
 * created on 2025-05-03
*/
class PropAccessUtilsTest {

    @ParameterizedTest
    @CsvSource(
        "prop1,prop1",
        "prop2,Prop2",
        "prop3,Prop3",
        "prop4,prop4",
        "prop5,Prop5",
        "prop6,", // static
        "prop7,", // getter with param
        "prop8,prop8",
        "prop9,noget-prop9",
    )
    fun test_getters(prop: String, expectedValue: String?) {
        val obj = Test1Pojo()
        obj.prop2 = "Prop2"
        obj.prop3 = "Prop3"
        obj.prop5 = "Prop5"
        val getter = PropAccessUtils.getGetterByName(Test1Pojo::class, prop, String::class.createType())
        assertEquals(expectedValue, getter?.call(obj))
    }

    @ParameterizedTest
    @CsvSource(
        "prop2,ok",
        "prop3,ok",
        "prop4,ok",
        "prop5,ok",
        "prop6,fail", // static
        "prop7,fail", // with 2nd param
        "prop8,fail", // w/o "set"
    )
    fun test_setters(prop: String, status: String) {
        val obj = Test1Pojo()
        val setter = PropAccessUtils.getSetterByName(Test1Pojo::class, prop, String::class.createType())
        val getter = PropAccessUtils.getGetterByName(Test1Pojo::class, prop, String::class.createType())
        if (status == "fail") {
            assertNull(setter, "expect to miss a setter")
        } else {
            assertNotNull(setter)
            setter!!.call(obj, "x")
            assertEquals("x", getter?.call(obj))
        }
    }
}
