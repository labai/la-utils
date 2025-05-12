import com.github.labai.utils.convert.LaConverterRegistry
import com.github.labai.utils.mapper.AutoMapper
import com.github.labai.utils.mapper.LaMapper
import com.github.labai.utils.mapper.LaMapper.LaMapperConfig
import com.github.labai.utils.mapper.impl.DataConverters
import com.github.labai.utils.mapper.impl.LaMapperAsmCompiler2.Compiled2AutoMapper
import com.github.labai.utils.mapper.impl.LaMapperAsmCompiler3
import com.github.labai.utils.mapper.impl.LaMapperAsmCompiler3.Compiled3AutoMapper
import com.github.labai.utils.mapper.impl.LaMapperImpl.AutoMapperImpl
import com.github.labai.utils.mapper.impl.ServiceContext
import jtest.StructuresInJava.Record12
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/*
 * @author Augustus
 * created on 2025-05-11
*/
class CompileTest {
    private val laMapperAsmCompiler3: LaMapperAsmCompiler3
    private val serviceContext: ServiceContext
    private val reflectionLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(disableCompile = true))
    private val compileLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(startCompileAfterIterations = 0))

    init {
        val config = LaMapperConfig()
        val dataConverters = DataConverters(LaConverterRegistry.global, config)
        serviceContext = ServiceContext().apply { this.config = config; this.dataConverters = dataConverters }
        laMapperAsmCompiler3 = LaMapperAsmCompiler3(serviceContext)
    }

    data class Pojo12(
        val v01: Long,
        val v02: Long,
        val v03: Int,
        val v04: Int,
        val v06: String,
    )

    private val pojo12 = Pojo12(1L, 2L, 3, 4, "6")
    private val record12 = Record12(1L, 2L, 3, 4, "6")

    @Test
    fun test_pojo_to_rec() {
        val mapper3 = getAsm3AutoMapper(Pojo12::class, Record12::class)
        val res = mapper3.transform(pojo12)
        assertEquals(record12, res)
    }

    @Test
    fun test_rec_to_pojo() {
        val mapper3 = getAsm3AutoMapper(Record12::class, Pojo12::class)
        val res = mapper3.transform(record12)
        assertEquals(pojo12, res)
    }

    @Test
    fun test_pojo_to_pojo() {
        val mapper3 = getAsm3AutoMapper(Pojo12::class, Pojo12::class)
        val res = mapper3.transform(pojo12)
        assertEquals(pojo12, res)
    }

    @Test
    fun test_rec_to_rec() {
        val mapper3 = getAsm3AutoMapper(Record12::class, Record12::class)
        val res = mapper3.transform(record12)
        assertEquals(record12, res)
    }

    private fun <Fr : Any, To : Any> getAsm3AutoMapper(sourceType: KClass<Fr>, targetType: KClass<To>): AutoMapper<Fr, To> {
        val refMapper = reflectionLaMapper.autoMapper(sourceType, targetType) as AutoMapperImpl
        refMapper.init()
        val struct = refMapper.struct
        return laMapperAsmCompiler3.compiledMapper(struct)
    }

    @JvmInline
    value class Type1(val i: Int)

    class PojoWithValue {
        var v01: Long? = null
        var v02: Type1 = Type1(2)
    }

    @Test
    fun test_compile_type() {
        val pojoMapper = compileLaMapper.autoMapper(Pojo12::class, Pojo12::class) as AutoMapperImpl
        assertIsAsm3(pojoMapper)

        val recordMapper = compileLaMapper.autoMapper(Record12::class, Record12::class) as AutoMapperImpl
        assertIsAsm3(recordMapper)

        val valueMapper = compileLaMapper.autoMapper(PojoWithValue::class, PojoWithValue::class) as AutoMapperImpl
        assertIsAsm2(valueMapper)

        val noFullCompileLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(startCompileAfterIterations = 0, disableFullCompile = true))
        val recordMapper2 = noFullCompileLaMapper.autoMapper(Record12::class, Record12::class) as AutoMapperImpl
        assertIsAsm2(recordMapper2)

        val noSynthConstrLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(startCompileAfterIterations = 0, disableSyntheticConstructorCall = true))
        val recordMapper3 = noSynthConstrLaMapper.autoMapper(Record12::class, Record12::class) as AutoMapperImpl
        assertIsAsm2(recordMapper3)
    }

    @Test
    fun test_compileException() {
        // default mapper ignores exception and leave ReflectionMapper
        val voidMapper1 = compileLaMapper.autoMapper(Void::class, Void::class) as AutoMapperImpl
        voidMapper1.init()

        // mapper with failOnOptimizationError=true
        val failOnErrorCompileLaMapper = LaMapper(LaConverterRegistry.global, LaMapperConfig().copy(startCompileAfterIterations = 0, failOnOptimizationError = true))
        val voidMapper2 = failOnErrorCompileLaMapper.autoMapper(Void::class, Void::class) as AutoMapperImpl
        assertThrowsExactly(IllegalArgumentException::class.java) {
            // throws: Class class java.lang.Void doesn't have no-argument constructor
            voidMapper2.init()
        }
    }

    private fun <Fr : Any, To : Any> assertIsAsm2(autoMapper: AutoMapperImpl<Fr, To>) {
        autoMapper.init()
        assertTrue(autoMapper.activeMapper is Compiled2AutoMapper)
    }

    private fun <Fr : Any, To : Any> assertIsAsm3(autoMapper: AutoMapperImpl<Fr, To>) {
        autoMapper.init()
        assertTrue(autoMapper.activeMapper is Compiled3AutoMapper)
    }
}
