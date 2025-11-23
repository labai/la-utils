package targetbuilder

import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.impl.DataConverters
import com.github.labai.utils.mapper.impl.ServiceContext
import com.github.labai.utils.targetbuilder.TargetBuilderJ
import com.github.labai.utils.targetbuilder.impl.TargetBuilderFactory
import com.github.labai.utils.targetbuilder.impl.TargetBuilderStringFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/*
 * @author Augustus
 * created on 2025-06-28
*/
class TargetBuilderTest {
    private val serviceContext: ServiceContext

    init {
        val config = LaMapper.LaMapperConfig()
        val dataConverters = DataConverters(LaConverterRegistry.global, config)
        serviceContext = ServiceContext().apply { this.config = config; this.dataConverters = dataConverters }
    }

    class Pojo1(
        var name: String,
        var age: Long = 5,
    ) {
        var address: String? = null
        val a00: String? = "x0"
        var a01: String = "x1"
        var a02: String? = null
        // var ldt: LocalDate? = null
    }

    @Test
    fun test_builder_simple() {
        val factory = TargetBuilderJ.forClass(Pojo1::class.java)
        val pojo: Pojo1 = factory.instance()
            .add("name", "Vardas")
            .add("age", 18)
            .add("address", "Vilnius")
            .add("a00", "a00")
            // .add("ldt", LocalDateTime.parse("2025-11-08T12:13:14"))
            .build()

        assertEquals("Vardas", pojo.name)
        assertEquals(18, pojo.age)
        assertEquals("Vilnius", pojo.address)
        assertEquals("x0", pojo.a00) // ignored update, as readonly (val)
        assertEquals("", pojo.a01) // empty, as not null (Q: is ok, if overwrites default?)
        // assertEquals("2025-11-08", pojo.ldt.toString())
    }

    @Test
    fun test_builder_from_strings_simple() {
        val factory = TargetBuilderJ.fromStringsSource(Pojo1::class.java)
        val pojo: Pojo1 = factory.instance()
            .add("name", "Vardas")
            .add("age", "18")
            .add("address", "Vilnius")
            .add("a00", "a00")
            // .add("ldt", "2025-11-08T12:13:14") // todo
            .build()

        assertEquals("Vardas", pojo.name)
        assertEquals(18, pojo.age)
        assertEquals("Vilnius", pojo.address)
        assertEquals("x0", pojo.a00) // ignored update, as readonly (val)
        assertEquals("", pojo.a01) // empty, as not null (Q: is ok, if overwrites default?)
        // assertEquals("2025-11-08", pojo.ldt.toString())
    }

    @JvmRecord
    data class Pojo2Record(
        val name: String,
        val age: Int = 5,
    )

    @Test
    fun test_builder_record() {
        val factory = TargetBuilderFactory(Pojo2Record::class, serviceContext)
        val pojo: Pojo2Record = factory.instance()
            .add("name", "Vardas")
            .add("age", 18)
            .build()

        assertEquals("Vardas", pojo.name)
        assertEquals(18, pojo.age)
    }

    @Test
    fun test_builder_from_strings_record() {
        val factory = TargetBuilderStringFactory(Pojo2Record::class, serviceContext)
        val pojo: Pojo2Record = factory.instance()
            .add("name", "Vardas")
            .add("age", "18")
            .build()

        assertEquals("Vardas", pojo.name)
        assertEquals(18, pojo.age)
    }
}
