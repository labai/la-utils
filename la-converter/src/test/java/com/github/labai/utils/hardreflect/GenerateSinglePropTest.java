package com.github.labai.utils.hardreflect;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Augustus
 * created on 2023-02-14
 */
@SuppressWarnings("unused")
public class GenerateSinglePropTest {

    @Test
    void test_reader_field_external() {
        Source1 pojo = new Source1();
        pojo.a01 = "bbb";
        assertReadValue(pojo, "a01", "bbb");
    }

    @Test
    void test_reader_Getter() {
        Source2 pojo = new Source2();
        pojo.setAaa("bbb");
        assertReadValue(pojo, "aaa", "bbb");
    }


    public static class Source3 {
        private String aaa;

        public String getAaa() {
            return aaa;
        }

        public void setAaa(String aaa) {
            this.aaa = aaa;
        }
    }

    @Test
    void test_reader_getter_nested_class() {
        Source3 pojo = new Source3();
        pojo.setAaa("bbb");
        assertReadValue(pojo, "aaa", "bbb");
    }

    public static class PojoStr {
        public String str = "str";
    }

    public static class PojoInt {
        public int iii = 5;
    }

    @Test
    void test_reader_read_string() {
        assertReadValue(new PojoStr(), "str", "str");
    }

    @Test
    void test_writer_write_string() {
        PojoStr pojo = new PojoStr();
        PropWriter<PojoStr> propWriter = LaHardReflect.createWriterClass(PojoStr.class, "str");
        propWriter.writeVal(pojo, "bbb");
        assertEquals("bbb", pojo.str);
    }

    @Test
    void test_reader_read_int() {
        assertReadValue(new PojoInt(), "iii", 5);
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

        public int getAint() {
            return aint;
        }

        public void setAint(int aint) {
            this.aint = aint;
        }

        public long getAlong() {
            return along;
        }

        public void setAlong(long along) {
            this.along = along;
        }

        public boolean isAboolean() {
            return aboolean;
        }

        public void setAboolean(boolean aboolean) {
            this.aboolean = aboolean;
        }

        public short getAshort() {
            return ashort;
        }

        public void setAshort(short ashort) {
            this.ashort = ashort;
        }

        public byte getAbyte() {
            return abyte;
        }

        public void setAbyte(byte abyte) {
            this.abyte = abyte;
        }

        public char getAchar() {
            return achar;
        }

        public void setAchar(char achar) {
            this.achar = achar;
        }

        public double getAdouble() {
            return adouble;
        }

        public void setAdouble(double adouble) {
            this.adouble = adouble;
        }

        public float getAfloat() {
            return afloat;
        }

        public void setAfloat(float afloat) {
            this.afloat = afloat;
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }
    }

    @Test
    void test_reader_various_fields() {
        VariousField pojo = new VariousField();
        assertReadValue(pojo, "aint", 5);
        assertReadValue(pojo, "along", 5L);
        assertReadValue(pojo, "aboolean", true);
        assertReadValue(pojo, "ashort", Short.valueOf("5"));
        assertReadValue(pojo, "abyte", Byte.valueOf("5"));
        assertReadValue(pojo, "adouble", 5d);
        assertReadValue(pojo, "afloat", 5f);
        assertReadValue(pojo, "str", "str");
    }

    @Test
    void test_reader_various_getters() {
        VariousGetterSetter pojo = new VariousGetterSetter();
        assertReadValue(pojo, "aint", 5);
        assertReadValue(pojo, "along", 5L);
        assertReadValue(pojo, "aboolean", true);
        assertReadValue(pojo, "ashort", Short.valueOf("5"));
        assertReadValue(pojo, "abyte", Byte.valueOf("5"));
        assertReadValue(pojo, "adouble", 5d);
        assertReadValue(pojo, "afloat", 5f);
        assertReadValue(pojo, "str", "str");
    }

    @Test
    void test_writer_various_fields() {
        VariousField pojo = new VariousField();
        assertWriteValue(pojo, "aint", 5, () -> pojo.aint);
        assertWriteValue(pojo, "along", 5L, () -> pojo.along);
        assertWriteValue(pojo, "aboolean", true, () -> pojo.aboolean);
        assertWriteValue(pojo, "ashort", Short.valueOf("5"), () -> pojo.ashort);
        assertWriteValue(pojo, "abyte", Byte.valueOf("5"), () -> pojo.abyte);
        assertWriteValue(pojo, "adouble", 5d, () -> pojo.adouble);
        assertWriteValue(pojo, "afloat", 5f, () -> pojo.afloat);
        assertWriteValue(pojo, "str", "str", () -> pojo.str);
    }

    @Test
    void test_writer_various_setters() {
        VariousGetterSetter pojo = new VariousGetterSetter();
        assertWriteValue(pojo, "aint", 5, () -> pojo.aint);
        assertWriteValue(pojo, "along", 5L, () -> pojo.along);
        assertWriteValue(pojo, "aboolean", true, () -> pojo.aboolean);
        assertWriteValue(pojo, "ashort", Short.valueOf("5"), () -> pojo.ashort);
        assertWriteValue(pojo, "abyte", Byte.valueOf("5"), () -> pojo.abyte);
        assertWriteValue(pojo, "adouble", 5d, () -> pojo.adouble);
        assertWriteValue(pojo, "afloat", 5f, () -> pojo.afloat);
        assertWriteValue(pojo, "str", "str", () -> pojo.str);
    }

    @SuppressWarnings("unchecked")
    <T, F> void assertReadValue(T pojo, String fieldName, F expected) {
        PropReader<T> propReader = LaHardReflect.createReaderClass((Class<T>) pojo.getClass(), fieldName);
        F res = (F) propReader.readVal(pojo);
        assertEquals(expected, res);
    }

    @SuppressWarnings("unchecked")
    <T, F> void assertWriteValue(T pojo, String fieldName, F value, Supplier<?> getFn) {
        try {
            PropWriter<T> propWriter = LaHardReflect.createWriterClass((Class<T>) pojo.getClass(), fieldName);
            propWriter.writeVal(pojo, value);
        } catch (Exception e) {
            fail("Can't assign field '" + fieldName + "' for pojo '" + pojo.getClass().getName() + "': " + e.getMessage());
        }
        assertEquals(value, getFn.get());
    }
}
