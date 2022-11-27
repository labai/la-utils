package com.github.labai.utils.convert.ext;

import com.github.labai.utils.convert.LaConverterRegistry;
import com.github.labai.deci.Deci;

import java.math.BigDecimal;
import java.math.BigInteger;

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
        registry.registerConverter(BigDecimal.class, Deci.class, Deci::new);
        registry.registerConverter(Deci.class, BigDecimal.class, Deci::toBigDecimal);
        registry.registerConverter(BigInteger.class, Deci.class, bigi -> new Deci(new BigDecimal(bigi)));
        registry.registerConverter(Deci.class, BigInteger.class, deci -> deci.toBigDecimal().toBigInteger());
        registry.registerConverter(Long.class, Deci.class, Deci::new);
        registry.registerConverter(long.class, Deci.class, Deci::new);
        registry.registerConverter(Deci.class, Long.class, Deci::toLong);
        registry.registerConverter(Deci.class, long.class, Deci::toLong);
        registry.registerConverter(Integer.class, Deci.class, Deci::new);
        registry.registerConverter(int.class, Deci.class, Deci::new);
        registry.registerConverter(Deci.class, Integer.class, Deci::toInt);
        registry.registerConverter(Deci.class, int.class, Deci::toInt);
        registry.registerConverter(String.class, Deci.class, Deci::new);
        registry.registerConverter(Deci.class, String.class, Deci::toString);
    }
}
