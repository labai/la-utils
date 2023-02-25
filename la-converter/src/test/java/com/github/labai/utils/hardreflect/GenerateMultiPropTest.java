package com.github.labai.utils.hardreflect;

import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Augustus
 * created on 2023-02-14
 */
@SuppressWarnings("unused")
public class GenerateMultiPropTest {

    private final String TEST_STR = "aaa";
    private final BigDecimal TEST_DEC = new BigDecimal("2.2");
    private final int TEST_INT = 2;

    @Test
    void test_reader_getter() {
        SourceMulti2 pojo = new SourceMulti2();
        pojo.setA1(TEST_STR);
        pojo.setA2(TEST_INT);
        pojo.setA3(TEST_DEC);

        PropMultiReader propMultiReader = LaHardReflect.createMultiReaderClass(pojo.getClass(), Arrays.asList("a1", "a2", "a3"));
        Object[] res = propMultiReader.readVals(pojo);
        assertEquals(TEST_STR, res[0]);
        assertEquals(TEST_INT, res[1]);
        assertEquals(TEST_DEC, res[2]);
    }


    public static class Source3 {
        private String a1;
        private int a2;
        private BigDecimal a3;

        public String getA1() {return a1;}
        public void setA1(String a1) {this.a1 = a1;}
        public int getA2() {return a2;}
        public void setA2(int a2) {this.a2 = a2;}
        public BigDecimal getA3() {return a3;}
        public void setA3(BigDecimal a3) {this.a3 = a3;}
    }

    @Test
    void test_reader_getter_nested_class() {
        Source3 pojo = new Source3();
        pojo.setA1(TEST_STR);
        pojo.setA2(TEST_INT);
        pojo.setA3(TEST_DEC);

        PropMultiReader propMultiReader = LaHardReflect.createMultiReaderClass(pojo.getClass(), Arrays.asList("a1", "a2", "a3"));
        Object[] res = propMultiReader.readVals(pojo);
        assertEquals(TEST_STR, res[0]);
        assertEquals(TEST_INT, res[1]);
        assertEquals(TEST_DEC, res[2]);
    }

    public static class VariousField {
        // primitives
        public int aint = 5;
        public long along = 5;
        public boolean aboolean = true;
        public short ashort = 5;
        public byte abyte = 5;
        public char achar = 5;
        public double adouble = 5;
        public float afloat = 5;

        public String str = "str";
    }

    public static class VariousGetterSetter {
        // primitives
        private int aint = 5;
        private long along = 5;
        private boolean aboolean = true;
        private short ashort = 5;
        private byte abyte = 5;
        private char achar = 5;
        private double adouble = 5;
        private float afloat = 5;
        private String str = "str";

        public int getAint() {return aint;}
        public void setAint(int aint) {this.aint = aint;}
        public long getAlong() {return along;}
        public void setAlong(long along) {this.along = along;}
        public boolean isAboolean() {return aboolean;}
        public void setAboolean(boolean aboolean) {this.aboolean = aboolean;}
        public short getAshort() {return ashort;}
        public void setAshort(short ashort) {this.ashort = ashort;}
        public byte getAbyte() {return abyte;}
        public void setAbyte(byte abyte) {this.abyte = abyte;}
        public char getAchar() {return achar;}
        public void setAchar(char achar) {this.achar = achar;}
        public double getAdouble() {return adouble;}
        public void setAdouble(double adouble) {this.adouble = adouble;}
        public float getAfloat() {return afloat;}
        public void setAfloat(float afloat) {this.afloat = afloat;}
        public String getStr() {return str;}
        public void setStr(String str) {this.str = str;}
    }

    @Test
    void test_reader_various_fields() {
        VariousField pojo = new VariousField();
        List<String> fieldNames = Arrays.asList("aint", "along", "aboolean", "ashort", "abyte", "adouble", "afloat", "str");
        Object[] expected = {
            5,
            5L,
            true,
            Short.valueOf("5"),
            Byte.valueOf("5"),
            5d,
            5f,
            "str",
        };
        PropMultiReader propMultiReader = LaHardReflect.createMultiReaderClass(pojo.getClass(), fieldNames);
        Object[] res = propMultiReader.readVals(pojo);
        assertArrayEquals(expected, res);
    }

    @Test
    void test_reader_various_getters() {
        VariousGetterSetter pojo = new VariousGetterSetter();
        List<String> fieldNames = Arrays.asList("aint", "along", "aboolean", "ashort", "abyte", "adouble", "afloat", "str");
        Object[] expected = {
            5,
            5L,
            true,
            Short.valueOf("5"),
            Byte.valueOf("5"),
            5d,
            5f,
            "str",
        };
        PropMultiReader propMultiReader = LaHardReflect.createMultiReaderClass(pojo.getClass(), fieldNames);
        Object[] res = propMultiReader.readVals(pojo);
        assertArrayEquals(expected, res);
    }

    @Test
    void test_writer_various_fields() {
        VariousField pojo = new VariousField();
        List<String> fieldNames = Arrays.asList("aint", "along", "aboolean", "ashort", "abyte", "adouble", "afloat", "str");
        Object[] values = {
            5,
            5L,
            true,
            Short.valueOf("5"),
            Byte.valueOf("5"),
            5d,
            5f,
            "str",
        };

        PropMultiWriter writer = LaHardReflect.createMultiWriterClass(pojo.getClass(), fieldNames);
        writer.writeVals(pojo, values);

        assertEquals(5, pojo.aint);
        assertEquals(5L, pojo.along);
        assertEquals(true, pojo.aboolean);
        assertEquals(Short.valueOf("5"), pojo.ashort);
        assertEquals(Byte.valueOf("5"), pojo.abyte);
        assertEquals(5d, pojo.adouble);
        assertEquals(5f, pojo.afloat);
        assertEquals("str", pojo.str);
    }

    @Test
    void test_writer_various_setters() {
        VariousGetterSetter pojo = new VariousGetterSetter();
        List<String> fieldNames = Arrays.asList("aint", "along", "aboolean", "ashort", "abyte", "adouble", "afloat", "str");
        Object[] values = {
            5,
            5L,
            true,
            Short.valueOf("5"),
            Byte.valueOf("5"),
            5d,
            5f,
            "str",
        };

        PropMultiWriter writer = LaHardReflect.createMultiWriterClass(pojo.getClass(), fieldNames);
        writer.writeVals(pojo, values);

        assertEquals(5, pojo.aint);
        assertEquals(5L, pojo.along);
        assertEquals(true, pojo.aboolean);
        assertEquals(Short.valueOf("5"), pojo.ashort);
        assertEquals(Byte.valueOf("5"), pojo.abyte);
        assertEquals(5d, pojo.adouble);
        assertEquals(5f, pojo.afloat);
        assertEquals("str", pojo.str);
    }

    @Test
    void test_setters_lamapper_props() {
        Source3 pojo = new Source3();
        pojo.setA1(TEST_STR);
        pojo.setA2(TEST_INT);
        pojo.setA3(TEST_DEC);

        List<String> fieldNames = Arrays.asList("a1", "a2", "a3");
        Object[] expected = {
            TEST_STR,
            TEST_INT,
            TEST_DEC
        };

        // reader
        List<NameOrAccessor> props = fieldNames.stream().map(NameOrAccessor::name).collect(Collectors.toList());
        PropMultiReader reader = LaHardReflect.createMultiReaderClassForProps(pojo.getClass(), props);
        Object[] values = reader.readVals(pojo);
        assertArrayEquals(expected, values);

        // writer
        PropMultiWriter writer = LaHardReflect.createMultiWriterClassForProps(pojo.getClass(), props);
        writer.writeVals(pojo, values);

        assertEquals(TEST_STR, pojo.a1);
        assertEquals(TEST_INT, pojo.a2);
        assertEquals(TEST_DEC, pojo.a3);
    }
}
