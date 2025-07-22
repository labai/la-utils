package com.github.labai.utils.convert;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Augustus
 * created on 2022.11.23
 */
@SuppressWarnings({"rawtypes", "unchecked", "SimplifiableAssertion"})
public class LaConvTypeTest {
    private final LaConverterRegistry laConverterRegistry = new LaConverterRegistry();

    private static final List<Class> numberClasses = Arrays.asList(
        Long.class,
        Integer.class,
        Double.class,
        Float.class,
        Byte.class,
        Short.class,
        BigInteger.class,
        BigDecimal.class
    );

    private static final List<Class> numberPrimitiveClasses = Arrays.asList(
        long.class,
        int.class,
        double.class,
        float.class,
        byte.class,
        short.class
    );

    private static final List<Class> dateClasses = Arrays.asList(
        LocalDate.class,
        LocalDateTime.class,
        OffsetDateTime.class,
        ZonedDateTime.class,
        Instant.class,
        java.sql.Date.class,
        Timestamp.class,
        Date.class,
        String.class
    );

    private static final List<Class> miscClasses = Arrays.asList(
        String.class,
        Character.class,
        Boolean.class,
        Enum.class
    );

    private static final List<Class> miscPrimitiveClasses = Arrays.asList(
        char.class,
        boolean.class
    );


    @SuppressWarnings({"rawtypes", "unchecked"})
    private void testConv(Object from, Object expected) {
        ITypeConverter converter = laConverterRegistry.getConverter(from.getClass(), expected.getClass());
        assertEquals(converter.convert(from), expected);
    }

    @Test
    public void test_conv_numbers_integer() {
        List<Object> fives = Arrays.asList(
            5L,
            5,
            (short) 5,
            (byte) 5,
            BigInteger.valueOf(5),
            new BigDecimal(5),
            5.0f,
            5.0
        );

        for (Object ofr : fives) {
            for (Object oto : fives) {
                testConvNum(ofr, oto);
            }
        }
    }

    @Test
    public void test_conv_number_string() {
        List<Object> fives = Arrays.asList(
            5L,
            5,
            (short) 5,
            (byte) 5,
            BigInteger.valueOf(5),
            new BigDecimal(5),
            "5"
        );

        for (Object ofr : fives) {
            for (Object oto : fives) {
                testConvNum(ofr, oto);
            }
        }
    }

    @Test
    public void test_conv_number_floating() {
        List<Object> fives = Arrays.asList(
            new BigDecimal("5.1"),
            5.1f,
            5.1,
            "5.1"
        );

        for (Object ofr : fives) {
            for (Object oto : fives) {
                testConvNumRounded(ofr, oto);
            }
        }
    }

    @Test
    public void test_conv_boolean() {
        List<Object> trues1 = Arrays.asList(
            true,
            1L,
            1,
            (short) 1,
            (byte) 1,
            BigInteger.valueOf(1),
            new BigDecimal("1"),
            1.0f,
            1.0,
            "true"
        );

        List<Object> trues5 = Arrays.asList(
            5L,
            5,
            (short) 5,
            (byte) 5,
            BigInteger.valueOf(5),
            new BigDecimal("5"),
            5.0f,
            5.0
        );

        List<Object> falses = Arrays.asList(
            false,
            0L,
            0,
            (short) 0,
            (byte) 0,
            BigInteger.valueOf(0),
            new BigDecimal("0"),
            0.0f,
            0.0,
            "false"
        );

        for (Object ofr : trues1) {
            testConvToBoolean(ofr, true);
            testConvFromBoolean(true, ofr);
        }
        for (Object ofr : trues5) {
            testConvToBoolean(ofr, true);
        }
        for (Object ofr : falses) {
            testConvToBoolean(ofr, false);
            testConvFromBoolean(false, ofr);
        }

        testConvToBoolean("abra", false);
    }

    private enum Enum1 {
        ABRA, KADABRA
    }

    private enum Enum2 {
        ABRA
    }

    @Test
    public void test_conv_enums() {
        testConv(Enum1.ABRA, Enum1.ABRA);
        testConv(Enum1.ABRA, "ABRA");
        testConv(Enum1.ABRA, Enum2.ABRA);
    }

    @Test
    public void test_conv_null_string() {
        List<Class> classes = new ArrayList<>();
        classes.addAll(numberClasses);
        classes.addAll(dateClasses);
        classes.addAll(miscClasses);

        for (Class clazz : classes) {
            testConvNull(clazz, String.class);
        }
    }

    @Test
    public void test_conv_null_number() {
        for (Class classTo : numberClasses) {
            for (Class classFr : numberClasses) {
                testConvNull(classFr, classTo);
            }
            testConvNull(String.class, classTo);
        }
    }

    @Test
    public void test_conv_null_date() {
        for (Class classFr : dateClasses) {
            for (Class classTo : dateClasses) {
                testConvNull(classFr, classTo);
            }
        }
    }

    @Test
    public void test_conv_null_misc() {
        testConvNull(Enum1.class, Enum2.class);
        testConvNull(String.class, Enum2.class);
        testConvNull(String.class, Boolean.class);
        testConvNull(String.class, Character.class);
    }

    private void testConvNull(Class classFr, Class classTo) {
        ITypeConverter converter = laConverterRegistry.getConverter(classFr, classTo);
        assertNull(converter.convert(null));
    }

    @SuppressWarnings({"rawtypes", "unchecked", "SimplifiableAssertion"})
    private void testConvNum(Object from, Object expected) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), expected.getClass());
        Object converted = converter.convert(from);
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expected.getClass() + ")";
        if (expected.getClass() == BigDecimal.class) {
            assertTrue(((BigDecimal) converted).compareTo((BigDecimal) expected) == 0, msg + ", value=" + converted + " expected=" + expected);
        } else {
            assertEquals(expected, converted, msg);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void testConvNumRounded(Object from, Object expected) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), expected.getClass());
        Object converted = converter.convert(from);
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expected.getClass() + ")";
        BigDecimal convertedBd = new BigDecimal(converted.toString());
        convertedBd = convertedBd.setScale(4, RoundingMode.HALF_UP);
        BigDecimal expectedBd = new BigDecimal(converted.toString());
        expectedBd = expectedBd.setScale(4, RoundingMode.HALF_UP);
        assertEquals(expectedBd, convertedBd, msg);
    }

    private void testConvToBoolean(Object from, Boolean expectedValue) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), Boolean.class);
        Object converted = converter.convert(from);
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expectedValue.getClass() + ")";
        assertEquals(expectedValue, converted, msg);
    }

    private void testConvFromBoolean(Boolean boolValue, Object expectedTarget) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(Boolean.class, expectedTarget.getClass());
        Object converted = converter.convert(boolValue);
        String msg = "failed conv " + boolValue.getClass() + " to " + expectedTarget.getClass();
        assertEquals(expectedTarget, converted, msg);
    }
}
