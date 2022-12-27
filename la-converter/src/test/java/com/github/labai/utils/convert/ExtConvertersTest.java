package com.github.labai.utils.convert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * @author Augustus
 * created on 2022-12-27
 */
public class ExtConvertersTest {

    @Test
    public void test_ext_resolver() {
        LaConverterRegistry convReg = new LaConverterRegistry();
        convReg.registerExtResolver(new TestResolver());
        ITypeConverter<Boolean, String> convFn = convReg.getConverter(Boolean.class, String.class);
        assertEquals("no", convFn.convert(false));
    }

    @Test
    public void test_ext_converter() {
        LaConverterRegistry convReg = new LaConverterRegistry();
        convReg.registerConverter(Boolean.class, String.class, (b) -> ((Boolean) b ? "Yep" : "Nop"));
        ITypeConverter<Boolean, String> convFn = convReg.getConverter(Boolean.class, String.class);
        assertEquals("Nop", convFn.convert(false));

    }

    private static class TestResolver implements IConverterResolver {
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public <Fr, To> ITypeConverter getConverter(Class<Fr> sourceType, Class<To> targetType) {
            if (sourceType == Boolean.class && targetType == String.class)
                return (b) -> ((Boolean) b ? "yes" : "no");
            return null;
        }
    }
}
