package com.github.labai.utils.hardreflect;

import com.github.labai.utils.hardreflect.LaHardCopy.PojoArgDef;
import com.github.labai.utils.hardreflect.LaHardCopy.PojoCopier;
import com.github.labai.utils.hardreflect.LaHardCopy.PojoCopyPropDef;
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor;
import com.github.labai.utils.hardreflect.impl.LaHardCopyImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Augustus
 * created on 2023-02-14
 */
@SuppressWarnings("unused")
public class GeneratePojoCopierTest {

    private final String TEST_STR = "str";
    private final BigDecimal TEST_DEC = new BigDecimal("2.2");
    private final int TEST_INT = 2;

    public static class Source01 {

        private String a01;
        private String a02;
        private String a03;
        private String a04;

        public String getA01() {return a01;}
        public void setA01(String a01) {this.a01 = a01;}
        public String getA02() {return a02;}
        public void setA02(String a02) {this.a02 = a02;}
        public String getA03() {return a03;}
        public void setA03(String a03) {this.a03 = a03;}
        public String getA04() {return a04;}
        public void setA04(String a04) {this.a04 = a04;}
    }

    public static class Target01 {
        private String a01;
        private String a02;
        private String a03;
        private String a04;

        public Target01(String a01, String a02) {
            this.a01 = a01;
            this.a02 = a02;
        }

        public Target01() {
        }

        public String getA01() {return a01;}
        public String getA02() {return a02;}
        public String getA03() {return a03;}
        public void setA03(String a03) {this.a03 = a03;}
        public String getA04() {return a04;}
        public void setA04(String a04) {this.a04 = a04;}

        @Override
        public String toString() {
            return "Target01{" +
                "a01='" + a01 + '\'' +
                ", a02='" + a02 + '\'' +
                ", a03='" + a03 + '\'' +
                ", a04='" + a04 + '\'' +
                '}';
        }
    }


    @Test
    void test_1_pojo_copier_constructor() {
        Source01 pojo = new Source01();
        pojo.setA01("aa1");
        pojo.setA02("aa2");
        pojo.setA03("aa3");
        pojo.setA04("aa4");

        List<PojoArgDef> argDefs = Arrays.asList(
            PojoArgDef.forProp(String.class, NameOrAccessor.name("a01")),
            PojoArgDef.forProp(String.class, NameOrAccessor.name("a02"), value -> value + "X2")
        );

        List<PojoCopyPropDef> propCopyDefs = Arrays.asList(
            PojoCopyPropDef.forAuto(
                NameOrAccessor.name("a03"),
                NameOrAccessor.name("a03"),
                value -> value + "X3"
            ),
            PojoCopyPropDef.forAuto(
                NameOrAccessor.name("a04"),
                NameOrAccessor.name("a04"),
                null
            )
        );

        PojoCopier<Source01, Target01> copier = LaHardCopyImpl.createPojoCopierClass(
            pojo.getClass(),
            Target01.class,
            // new ArrayList<>(),
            argDefs,
            propCopyDefs
            );
        Target01 res = copier.copyPojo(pojo);
        System.out.println("res " + res);
    }


    public static class Source02 {
        public int a01;
        public Integer a02;
        public int a03;
        public Integer a04;
    }

    public static class Target02 {
        public Integer a01;
        public int a02;
        public Integer a03;
        public int a04;

        public Target02(Integer a01, int a02) {
            this.a01 = a01;
            this.a02 = a02;
        }

        @Override
        public String toString() {
            return "Target02{a01=" + a01 + ", a02=" + a02 + ", a03=" + a03 + ", a04=" + a04 + '}';
        }
    }

    @Test
    void test_2_pojo_copier_with_boxing() {
        Source02 pojo = new Source02();
        pojo.a01 = 2;
        pojo.a02 = 2;
        pojo.a03 = 2;
        pojo.a04 = 2;

        List<PojoArgDef> argDefs = Arrays.asList(
            PojoArgDef.forProp(Integer.class, NameOrAccessor.name("a01")),
            PojoArgDef.forProp(int.class, NameOrAccessor.name("a02"))
        );

        List<PojoCopyPropDef> propCopyDefs = Arrays.asList(
            PojoCopyPropDef.forAuto(
                NameOrAccessor.name("a03"),
                NameOrAccessor.name("a03"),
                null
            ),
            PojoCopyPropDef.forAuto(
                NameOrAccessor.name("a04"),
                NameOrAccessor.name("a04"),
                null
            )
        );

        PojoCopier<Source02, Target02> copier = LaHardCopy.createPojoCopierClass(
            pojo.getClass(),
            Target02.class,
            argDefs,
            propCopyDefs
        );
        Target02 res = copier.copyPojo(pojo);

        System.out.println("res " + res);
        assertEquals(2, res.a01);
        assertEquals(2, res.a02);
        assertEquals(2, res.a03);
        assertEquals(2, res.a04);
    }


    public static class Source03 {
        public int a01;
        public Integer a02;
        public int a03;
        public Integer a04;
    }

    public static class Target03 {
        public String a01;
        public String a02;
        public String a03;
        public String a04;

        public Target03(String a01, String a02) {
            this.a01 = a01;
            this.a02 = a02;
        }

        public Target03() {
        }

        @Override
        public String toString() {
            return "Target02{a01=" + a01 + ", a02=" + a02 + ", a03=" + a03 + ", a04=" + a04 + '}';
        }
    }

    @Test
    void test_3_pojo_copier_with_conversion() {
        Source03 pojo = new Source03();
        pojo.a01 = 2;
        pojo.a02 = 2;
        pojo.a03 = 2;
        pojo.a04 = 2;

        List<PojoArgDef> argDefs = Arrays.asList(
            PojoArgDef.forProp(String.class, NameOrAccessor.name("a01"), value -> value.toString()),
            PojoArgDef.forProp(String.class, NameOrAccessor.name("a02"), value -> value.toString())
        );

        List<PojoCopyPropDef> propCopyDefs = Arrays.asList(
            PojoCopyPropDef.forAuto(
                NameOrAccessor.name("a03"),
                NameOrAccessor.name("a03"),
                value -> value.toString()
            ),
            PojoCopyPropDef.forAuto(
                NameOrAccessor.name("a04"),
                NameOrAccessor.name("a04"),
                value -> value.toString()
            )
        );

        PojoCopier<Source03, Target03> copier = LaHardCopy.createPojoCopierClass(
            pojo.getClass(),
            Target03.class,
            argDefs,
            propCopyDefs
        );
        Target03 res = copier.copyPojo(pojo);

        System.out.println("res " + res);
        assertEquals("2", res.a01);
        assertEquals("2", res.a02);
        assertEquals("2", res.a03);
        assertEquals("2", res.a04);
    }

    @Test
    void test_4_pojo_copier_with_dataSupplier() {
        Source03 pojo = new Source03();
        pojo.a01 = 2;
        pojo.a02 = 2;
        pojo.a03 = 2;
        pojo.a04 = 2;

        List<PojoArgDef> argDefs = Arrays.asList(
            PojoArgDef.forSupplier(String.class, fr -> "aax1", null),
            PojoArgDef.forSupplier(String.class, fr -> 5, value -> value.toString())
        );

        List<PojoCopyPropDef> propCopyDefs = Arrays.asList(
            PojoCopyPropDef.forManual(fr -> "aax2", NameOrAccessor.name("a03"), null),
            PojoCopyPropDef.forManual(fr -> 6, NameOrAccessor.name("a04"), value -> value.toString())
        );

        PojoCopier<Source03, Target03> copier = LaHardCopy.createPojoCopierClass(
            pojo.getClass(),
            Target03.class,
            argDefs,
            propCopyDefs
        );
        Target03 res = copier.copyPojo(pojo);

        System.out.println("res " + res);
        assertEquals("aax1", res.a01);
        assertEquals("5", res.a02);
        assertEquals("aax2", res.a03);
        assertEquals("6", res.a04);
    }

    public static class Target05 {
        public Integer a01;
        public int a02;

        public Target05(Integer a01, int a02) {
            this.a01 = a01;
            this.a02 = a02;
        }
    }

    @Test
    void test_5_pojo_copier_with_constants() {
        List<PojoArgDef> argDefs = Arrays.asList(
            PojoArgDef.forConstant(Integer.class, 1),
            PojoArgDef.forConstant(int.class, 1)
        );

        Source03 pojo = new Source03();

        PojoCopier<Source03, Target05> copier = LaHardCopy.createPojoCopierClass(
            pojo.getClass(),
            Target05.class,
            argDefs,
            new ArrayList<>()
        );
        Target05 res = copier.copyPojo(pojo);

        System.out.println("res " + res);
        assertEquals(1, res.a01);
        assertEquals(1, res.a02);
    }

    public record Record06(
        Integer a01,
        int a02
    ) {
    }

    @Test
    void test_6_pojo_copier_to_record() {
        List<PojoArgDef> argDefs = Arrays.asList(
            PojoArgDef.forProp(Integer.class, NameOrAccessor.name("a01")),
            PojoArgDef.forProp(int.class, NameOrAccessor.name("a02"))
        );

        Record06 fr = new Record06(1, 1);

        PojoCopier<Record06, Record06> copier = LaHardCopy.createPojoCopierClass(
            fr.getClass(),
            Record06.class,
            argDefs,
            new ArrayList<>()
        );
        Record06 res = copier.copyPojo(fr);

        System.out.println("res " + res);
        assertEquals(1, res.a01);
        assertEquals(1, res.a02);
    }
}
