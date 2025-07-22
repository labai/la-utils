package com.github.labai.utils.reflect;

import com.github.labai.deci.Deci;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 *         created on 2018.12.26
 *
 */
public class LaReflectExtTest {

    @Test
    public void testConvDeci() {
        class Pojo {
            Deci deci = new Deci("12.2");
            BigDecimal bigd = new BigDecimal("15.5");
        }

        Pojo pojo = new Pojo();
        Map<String, Field> fields = LaReflect.analyzeObjFields(pojo);
        LaReflect.assignValue(pojo, fields.get("deci"), new BigDecimal("4.4"));
        LaReflect.assignValue(pojo, fields.get("bigd"), new Deci("5.5"));

        assertEquals(new Deci("4.4"), pojo.deci);
        assertEquals(new BigDecimal("5.5"), pojo.bigd);
    }
}
