package com.github.labai.utils.convert;

import com.github.labai.deci.Deci;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2018.12.26
 */
public class LaConverterRegistryExtTest {
    private final LaConverterRegistry laConverterExtType = new LaConverterRegistry();
    {
        laConverterExtType.registerConverter(Logical.class, Boolean.class, log -> log == null ? null : log == Logical.YES);
        laConverterExtType.registerConverter(Boolean.class, Logical.class, bool -> (bool == null) ? null : (bool ? Logical.YES : Logical.NO));
    }

    enum Logical {YES, NO}

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void testConvExt(Object from, Object expected) {
        ITypeConverter converter = laConverterExtType.getConverter(from.getClass(), expected.getClass());
        assertEquals(converter.convert(from), expected);
    }

    @Test
    public void test_conv_ext_deci() {
        List<Object> fives = Arrays.asList(
                new BigDecimal(5),
                new Deci(5));

        for (Object ofr : fives) {
            for (Object oto : fives) {
                testConvExt(ofr, oto);
            }
        }
    }

    @Test
    public void test_conv_ext_logical() {
        testConvExt(true, Logical.YES);
        testConvExt(false, Logical.NO);
    }

}
