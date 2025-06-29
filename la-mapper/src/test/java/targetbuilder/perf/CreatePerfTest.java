package targetbuilder.perf;

import com.github.labai.utils.convert.LaConverterRegistry;
import com.github.labai.utils.mapper.LaMapper;
import com.github.labai.utils.mapper.impl.DataConverters;
import com.github.labai.utils.mapper.impl.ServiceContext;
import com.github.labai.utils.targetbuilder.impl.TargetBuilderStringFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/*
 * @author Augustus
 * created on 2025-06-25
 */
public class CreatePerfTest {

    public static class Pojo1 {
        public String name;
        public String address;
        public int age;
        public String a00;
        public String a01;
        public String a02;
        public String a03;
        public String a04;
        public String a05;
        public String a06;
        public String a07;
        public String a08;
        public String a09;
        public String a10;
        public String a11;
        public String a12;
        public String a13;
        public String a14;
        public String a15;
        public String a16;
        public String a17;
        public String a18;
        public String a19;
    }


    private static ServiceContext getServiceContext() {
        var ctx = new ServiceContext();
        ctx.config = new LaMapper.LaMapperConfig();
        ctx.dataConverters = new DataConverters(LaConverterRegistry.global, ctx.config);
        return ctx;
    }

    TargetBuilderStringFactory<Pojo1> getTargetBuilderFactory() {
        var klass = kotlin.jvm.JvmClassMappingKt.getKotlinClass(Pojo1.class);
        return new TargetBuilderStringFactory<>(klass, getServiceContext());
    }

    @Disabled("performance")
    @Test
    void test_perf_builder() {
        var factory = getTargetBuilderFactory();

        // warmup
        repeatCreate(1000, factory);

        var builderMs = repeatCreate(1_000_000, factory);
        var manualMs = repeatCreateAssign(1_000_000);
        System.out.println("1mln rec: builder=" + builderMs + "ms vs manual=" + manualMs + "ms");
    }

    private long repeatCreate(int count, TargetBuilderStringFactory<Pojo1> factory) {
        long startTs = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            var pojo = factory.instance()
                .add("name", "Vardas")
                .add("address", "Vilnius")
                .add("age", "18")
                .add("a00", "a00")
                .add("a01", "a01")
                .add("a02", "a02")
                .add("a03", "a03")
                .add("a04", "a04")
                .add("a05", "a05")
                .add("a06", "a06")
                .add("a07", "a07")
                .add("a08", "a08")
                .add("a09", "a09")
                .add("a10", "a10")
                .add("a11", "a11")
                .add("a12", "a12")
                .add("a13", "a13")
                .add("a14", "a14")
                .add("a15", "a15")
                .add("a16", "a16")
                .add("a17", "a17")
                .add("a18", "a18")
                .add("a19", "a19")
                .build();
        }
        return System.currentTimeMillis() - startTs;
    }

    private long repeatCreateAssign(int count) {
        long startTs = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            Pojo1 p = createPojoSample();
        }
        return System.currentTimeMillis() - startTs;
    }

    private static Pojo1 createPojoSample() {
        var pojo = new Pojo1();
        pojo.name = "Vardas";
        pojo.address = "Vilnius";
        pojo.age = 18;
        pojo.a00 = "a00";
        pojo.a01 = "a01";
        pojo.a02 = "a02";
        pojo.a03 = "a03";
        pojo.a04 = "a04";
        pojo.a05 = "a05";
        pojo.a06 = "a06";
        pojo.a07 = "a07";
        pojo.a08 = "a08";
        pojo.a09 = "a09";
        pojo.a10 = "a10";
        pojo.a11 = "a11";
        pojo.a12 = "a12";
        pojo.a13 = "a13";
        pojo.a14 = "a14";
        pojo.a15 = "a15";
        pojo.a16 = "a16";
        pojo.a17 = "a17";
        pojo.a18 = "a18";
        pojo.a19 = "a19";
        return pojo;
    }
}
