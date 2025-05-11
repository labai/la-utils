package com.github.labai.utils.reflect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author Augustus
 *         created on 2019.04.13
 */
public class LaPojoTest {

    static class Pojo1 {
        String aaa;
    }

    @Test
    public void testUsesClassCache() {
        Pojo1 pojo = new Pojo1();
        Map<String, Field> map1 = LaPojo.getCachedFieldsMap(pojo);
        Map<String, Field> map2 = LaPojo.getCachedFieldsMap(pojo);

        Assertions.assertSame(map1.get("aaa"), map2.get("aaa"));
    }
}
