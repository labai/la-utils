package com.github.labai.utils.convert.ext;

import com.github.labai.deci.Deci;
import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.convert.LaConverterRegistry;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

/**
 * @author Augustus
 * created on 2022.11.23
 */
public class DeciConverters {

    public static void registryDeciConverters(LaConverterRegistry registry) {
        boolean exists;
        try {
            Class.forName("com.github.labai.deci.Deci");
            exists = true;
        } catch (ClassNotFoundException e) {
            exists = false;
        }
        if (!exists)
            return;
        registerToDeci(registry, BigDecimal.class, Deci::new);
        registerToDeci(registry, BigInteger.class, bigi -> new Deci(new BigDecimal(bigi)));
        registerToDeci(registry, Long.class, Deci::new);
        registerToDeci(registry, long.class, Deci::new);
        registerToDeci(registry, Integer.class, Deci::new);
        registerToDeci(registry, int.class, Deci::new);
        registerToDeci(registry, String.class, Deci::new);

        registerFromDeci(registry, BigDecimal.class, Deci::toBigDecimal);
        registerFromDeci(registry, BigInteger.class, deci -> deci.toBigDecimal().toBigInteger());
        registerFromDeci(registry, Long.class, Deci::toLong);
        registerFromDeci(registry, long.class, Deci::toLong);
        registerFromDeci(registry, Integer.class, Deci::toInt);
        registerFromDeci(registry, int.class, Deci::toInt);
        registerFromDeci(registry, String.class, Deci::toString);
    }

    private static <Fr> void registerToDeci(LaConverterRegistry registry, Class<Fr> sourceType, Function<Fr, Deci> convFn) {
        registry.registerConverter(sourceType, Deci.class, withNullCheck(convFn));
    }

    private static <To> void registerFromDeci(LaConverterRegistry registry, Class<To> targetType, Function<Deci, To> convFn) {
        registry.registerConverter(Deci.class, targetType, withNullCheck(convFn));
    }

    private static <Fr, To> ITypeConverter<Fr, To> withNullCheck(Function<Fr, To> convFn) {
        return new ITypeConverter<Fr, To>() {
            @Override
            public @Nullable To convert(@Nullable Fr from) {
                return from == null ? null : convFn.apply(from);
            }
        };
    }
}
