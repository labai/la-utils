package com.github.labai.utils.convert;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Augustus
 * created on 2018.12.26
 *
 * test LaConvert.ConvNum
 *
 */
public class LaConverterStdNumTest {

    @Test
    public void test_conv_numbers() {
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

    @SuppressWarnings({"rawtypes", "unchecked", "SimplifiableAssertion"})
    private void testConvNum(Object from, Object expected) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), expected.getClass());
        Object converted = converter.convert(from);
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expected.getClass() + ")";
        if (expected.getClass() == BigDecimal.class) {
            assertTrue(((BigDecimal)converted).compareTo((BigDecimal) expected) == 0, msg);
        } else {
            assertEquals(converted, expected, msg);
        }
    }
}
