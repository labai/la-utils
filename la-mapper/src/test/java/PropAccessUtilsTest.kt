import com.github.labai.utils.mapper.impl.PropAccessUtils
import jtest.StructuresInJava.Test1Pojo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        "prop9,get-prop9",
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
        "prop2",
        "prop3",
        "prop4",
        "prop5",
    )
    fun test_setters(prop: String) {
        val obj = Test1Pojo()
        val setter = PropAccessUtils.getSetterByName(Test1Pojo::class, prop, String::class.createType())
        assertNotNull(setter)
        val getter = PropAccessUtils.getGetterByName(Test1Pojo::class, prop, String::class.createType())
        setter!!.call(obj, "x")
        assertEquals("x", getter?.call(obj))
    }
}
