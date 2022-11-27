package com.github.labai.utils.convert;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Augustus
 * created on 2022.11.23
 */
@SuppressWarnings({"rawtypes", "unchecked", "SimplifiableAssertion"})
public class LaConvTypeTest {
    private final LaConverterRegistry laConverterRegistry = new LaConverterRegistry();

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

    @SuppressWarnings({"rawtypes", "unchecked", "SimplifiableAssertion"})
    private void testConvNum(Object from, Object expected) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), expected.getClass());
        Object converted = converter.convert(from);
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expected.getClass() + ")";
        if (expected.getClass() == BigDecimal.class) {
            assertTrue(msg + ", value=" + converted + " expected=" + expected, ((BigDecimal)converted).compareTo((BigDecimal) expected) == 0);
        } else {
            assertEquals(msg, expected, converted);
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
        assertEquals(msg, expectedBd, convertedBd);
    }

    private void testConvToBoolean(Object from, Boolean expectedValue) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), Boolean.class);
        Object converted = converter.convert(from);
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expectedValue.getClass() + ")";
        assertEquals(msg, expectedValue, converted);
    }

    private void testConvFromBoolean(Boolean boolValue, Object expectedTarget) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(Boolean.class, expectedTarget.getClass());
        Object converted = converter.convert(boolValue);
        String msg = "failed conv " + boolValue.getClass() + " to " + expectedTarget.getClass();
        assertEquals(msg, expectedTarget, converted);
    }

}
