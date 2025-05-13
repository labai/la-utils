package com.github.labai.utils.hardreflect.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * @author Augustus
 * created on 2025-05-13
 */
public class UtilsTest {

    static class Pojo {
        public String getA01() {return "a01";}
        public String a02() {return "a02";}
        public String isA03() {return "a03";}
        public Boolean isA04() {return true;}
        public boolean isA05() {return true;}
        public static String getA06() {return "a06";}
        private String getA07() {return "a07";}

        public void setA01(String a01) { }
        public void a02(String a02) { }
        public static void setA06(String a06) { }
        private void setA07(String a07) { }
    }

    @ParameterizedTest
    @CsvSource({
        "a01,true",
        "a02,true", // w/o prefix
        "a03,false", // not boolean
        "a04,true",
        "a05,true",
        "a06,false", // static
        "a07,true" // even private(?)
    })
    void test_getter(String prop, boolean shouldExist) {
        if (shouldExist) {
            assertNotNull(Utils.getGetter(Pojo.class, prop), "expect to exist");
        } else {
            assertNull(Utils.getGetter(Pojo.class, prop), "expect to not exist");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "a01,true",
        "a02,true", // w/o prefix
        "a06,false", // static
        "a07,true" // even private(?)
    })
    void test_setter(String prop, boolean shouldExist) {
        if (shouldExist) {
            assertNotNull(Utils.getSetter(Pojo.class, prop), "expect to exist");
        } else {
            assertNull(Utils.getSetter(Pojo.class, prop), "expect to not exist");
        }
    }
}
