package com.github.labai.utils.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Augustus
 *         created on 2018.12.26
 *
 */
public class LaReflectTest {

    @Test
    public void testAssignValueSimple() {
        class Pojo {
            Integer int1 = 22;
            int int2 = 22;
            private Long long1 = 22L;
            private long long2 = 22;
        }
        Pojo pojo = new Pojo();
        Map<String, Field> fields = LaReflect.analyzeObjFields(pojo);
        LaReflect.assignValue(pojo, fields.get("int1"), 11);
        LaReflect.assignValue(pojo, fields.get("int2"), 11);
        LaReflect.assignValue(pojo, fields.get("long1"), 11);
        LaReflect.assignValue(pojo, fields.get("long2"), 11);
        assertEquals(Integer.valueOf(11), pojo.int1);
        assertEquals(11, pojo.int2);
        assertEquals(Long.valueOf(11), pojo.long1);
        assertEquals(11, pojo.long2);
    }


    @Test
    public void testAssignValueNulls() {
        class Pojo {
            Integer int1 = 22;
            int int2 = 22;
            Long long1 = 22L;
            long long2 = 22;
        }
        Pojo pojo = new Pojo();
        Map<String, Field> fields = LaReflect.analyzeObjFields(pojo);
        LaReflect.assignValue(pojo, fields.get("int1"), null);
        LaReflect.assignValue(pojo, fields.get("int2"), null);
        LaReflect.assignValue(pojo, fields.get("long1"), null);
        LaReflect.assignValue(pojo, fields.get("long2"), null);
        assertNull(pojo.int1);
        assertEquals(0, pojo.int2);
        assertNull(pojo.long1);
        assertEquals(0, pojo.long2);
    }

    @Test
    public void testAssignValueBoolean() {
        class Pojo {
            Boolean bool1 = false;
            boolean bool2 = false;
        }
        Pojo pojo = new Pojo();
        Map<String, Field> fields = LaReflect.analyzeObjFields(pojo);
        LaReflect.assignValue(pojo, fields.get("bool1"), true);
        LaReflect.assignValue(pojo, fields.get("bool2"), true);
        assertEquals(true, pojo.bool1);
        assertEquals(true, pojo.bool2);
    }


    private enum Enum1 {
        ABRA, KADABRA
    }

    @Test
    public void testAssignValueEnum() {
        class Pojo {
            Enum1 enum11 = null;
            Enum1 enum12 = null;
        }
        Pojo pojo = new Pojo();
        Map<String, Field> fields = LaReflect.analyzeObjFields(pojo);
        LaReflect.assignValue(pojo, fields.get("enum11"), Enum1.ABRA);
        LaReflect.assignValue(pojo, fields.get("enum12"), "KADABRA");
        assertEquals(Enum1.ABRA, pojo.enum11);
        assertEquals(Enum1.KADABRA, pojo.enum12);
    }

    @Test
    public void testAssignValueDates() throws ParseException {
        class Pojo {
            Date date1;
            java.sql.Date sqlDate1;
            Timestamp timestamp1;
            LocalDate ldt1;
            LocalDateTime ldtm1;
            OffsetDateTime odtm1;
            ZonedDateTime zdtm1;
        }
        class Checker {
            final String expectedDate;
            final String expectedDateTime;
            final Date date;

            Checker(String expectedDate, String expectedDateTime, Date date) {
                this.expectedDate = expectedDate;
                this.expectedDateTime = expectedDateTime;
                this.date = date;
            }

            void checkDateAssign(Object val_20181108_111213) {
                Pojo pojo = new Pojo();
                Map<String, Field> fields = LaReflect.analyzeObjFields(pojo);
                LaReflect.assignValue(pojo, fields.get("date1"), val_20181108_111213);
                LaReflect.assignValue(pojo, fields.get("sqlDate1"), val_20181108_111213);
                LaReflect.assignValue(pojo, fields.get("timestamp1"), val_20181108_111213);
                LaReflect.assignValue(pojo, fields.get("ldt1"), val_20181108_111213);
                LaReflect.assignValue(pojo, fields.get("ldtm1"), val_20181108_111213);
                LaReflect.assignValue(pojo, fields.get("odtm1"), val_20181108_111213);
                LaReflect.assignValue(pojo, fields.get("zdtm1"), val_20181108_111213);

                assertEquals(expectedDateTime, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(pojo.date1));
                assertEquals(expectedDate, new SimpleDateFormat("yyyy-MM-dd").format(pojo.sqlDate1));
                assertEquals(expectedDateTime, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(pojo.timestamp1));
                assertEquals(LocalDate.parse(expectedDate), pojo.ldt1);
                assertEquals(LocalDateTime.parse(expectedDateTime), pojo.ldtm1);
                assertEquals(OffsetDateTime.parse(expectedDateTime + getCurrentTimezoneOffset(date)), pojo.odtm1);
            }

            private String getCurrentTimezoneOffset(Date date) {
                int offsetInMillis = TimeZone.getDefault().getOffset(date.getTime());
                String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
                offset = (offsetInMillis >= 0 ? "+" : "-") + offset;
                return offset;
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date1 = sdf.parse("2018-11-08 11:12:13");

        Checker checkerDateTime = new Checker("2018-11-08", "2018-11-08T11:12:13", date1);
        checkerDateTime.checkDateAssign(date1);
        checkerDateTime.checkDateAssign(LocalDateTime.parse("2018-11-08T11:12:13"));
        checkerDateTime.checkDateAssign(new Timestamp(date1.getTime()));
        checkerDateTime.checkDateAssign(new java.sql.Date(date1.getTime())); // ?? sql date has time with sec also?
        checkerDateTime.checkDateAssign(new java.sql.Date(date1.getTime())); // ?? sql date has time with sec also?

        Checker checkerDate = new Checker("2018-11-08", "2018-11-08T00:00:00", date1);
        checkerDate.checkDateAssign(LocalDate.parse("2018-11-08"));
    }


    private static class Super1 {
        public String s1 = "s1";
        String s2 = "s2";
        private String s3 = "s3";
        public String s4 = "s4_super";
        public static String s6 = "s6";
    }

    private static class Main1 extends Super1 {
        private String s4 = "s4_main";
        private String s5 = "s5";
        public static String s7 = "s7";
    }



    @Test
    public void testAnalyzeFields() throws IllegalAccessException {
        Main1 obj = new Main1();
        Map<String, Field> fmap = LaReflect.analyzeObjFields(obj);

        // will be taken a) all field from Main object, b) public - from Super
        assertEquals(new HashSet<>(asList("s4", "s5")), fmap.keySet()); // TODO ? to use field from super class?
        // will be used child's private field
        assertEquals("s4_main", fmap.get("s4").get(obj));

        Super1 sup = new Super1();
        Map<String, Field> fmapSup = LaReflect.analyzeObjFields(sup);
        assertEquals(new HashSet<>(asList("s1", "s2", "s3", "s4")), fmapSup.keySet());
        assertEquals("s4_super", fmapSup.get("s4").get(obj));
    }
}
