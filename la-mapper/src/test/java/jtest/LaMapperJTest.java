package jtest;

import com.github.labai.utils.mapper.LaMapper;
import com.github.labai.utils.mapper.LaMapperJ;
import jtest.StructuresInJava.Record12;
import jtest.StructuresInJava.Test1Pojo;
import kotlin.Pair;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.labai.utils.mapper.LaMapperJ.mapFrom;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2023.11.22
 */
class LaMapperJTest {

    public static class ExamplePerson {
        public Integer id;
        public Long personCode;
        public LocalDateTime regDate;
        public String firstName;
        public String surname;
    }

    public static class ExamplePersonDto {
        public Long id;
        public String code;
        public LocalDate regDate;
        public String name;
    }

    @Test
    void test_example() {
        ExamplePerson from = new ExamplePerson();
        from.id = 101;
        from.personCode = 123456L;
        from.regDate = LocalDateTime.parse("2022-11-08T12:20:00");
        from.firstName = "Foo";
        from.surname = "Boo";

        ExamplePersonDto to = LaMapperJ.copyFrom(from, ExamplePersonDto.class, List.of(
            mapFrom("code", f -> f.personCode),
            mapFrom("name", f -> f.firstName + " " + f.surname)
        ));

        assertEquals(101, to.id);
        assertEquals(LocalDate.parse("2022-11-08"), to.regDate);
        assertEquals("123456", to.code);
        assertEquals("Foo Boo", to.name);
    }

    @Test
    void test1_field_copy() {
        var from = getMock();

        var mapper = LaMapperJ.autoMapper(Test1Pojo.class, Test1Pojo.class);

        var res = mapper.transform(from);
        assertEquals("f1", res.field1); // public field
        assertEquals(null, res.retrieveField2()); // private field. DON'T read it
        assertEquals("prop1", res.getProp1()); // getter only - no assign, leave default
        assertEquals("p2", res.getProp2()); // getter and setter
        assertEquals("p3", res.prop3); // public field, getter and setter
        assertEquals("p4", res.getProp4());
        assertEquals("p5", res.getProp5());
        assertEquals(null, res.getWrong1()); // field must much getter name in Java
        assertEquals(null, res.getWrong2()); // field must much getter type in Java
    }

    @Test
    void test1_field_map_combinations() {
        var from = getMock();

        var mapper = LaMapperJ.autoMapper(Test1Pojo.class, Test1Pojo.class, asList(
            mapFrom("field1", Test1Pojo::retrieveField2),
            mapFrom("field2", f -> f.field1),
            mapFrom("prop1", f -> f.getProp5()),
            mapFrom("prop2", Test1Pojo::getProp4),
            mapFrom("prop3", f -> f.prop3),
            mapFrom("prop4", Test1Pojo::getProp2),
            mapFrom("prop5", Test1Pojo::getProp1),
            mapFrom("wrong1", Test1Pojo::getWrong2),
            mapFrom("wrong2", Test1Pojo::getWrong1)
        ));

        var res = mapper.transform(from);
        assertEquals("f2", res.field1); // public field
        assertEquals(null, res.retrieveField2()); // private field. DON'T read it
        assertEquals("prop1", res.getProp1()); // getter only - no assign, leave default
        assertEquals("p4", res.getProp2()); // getter and setter
        assertEquals("p3", res.prop3); // public field, getter and setter
        assertEquals("p2", res.getProp4());
        assertEquals("prop1", res.getProp5());
        assertEquals(null, res.getWrong1()); // field must much getter name in Java
        assertEquals(null, res.getWrong2()); // field must much getter type in Java
    }

    @Test
    void test2_record_copy() {
        var from = new Record12(1L, 2, 3, 4, "a");

        var mapper = LaMapperJ.autoMapper(Record12.class, Record12.class);

        var res = mapper.transform(from);

        assertEquals(from, res);
    }

    @Test
    void test2_record_build() {
        var mapper = LaMapperJ.autoMapper(Object.class, Record12.class, asList(
            mapFrom("v01", f -> 1),
            mapFrom("v02", f -> 2),
            mapFrom("v03", f -> 3),
            mapFrom("v04", f -> null),
            mapFrom("v06", f -> "a")
        ));


        var expected = new Record12(1L, 2, 3, 0, "a");

        var res = mapper.transform(new Object());

        assertEquals(expected, res);
    }

    @Test
    void test3_copyFromMap() {
        var map = Map.<String, Object>of(
            "v01", 1,
            "v02", 2,
            "v03", 3L,
            "v04", 4L,
            "v06", "a");

        var map2 = new HashMap<>(map);
        map2.put("v04", null);

        var expected = new Record12(1L, 2, 3, 0, "a");

        var res = copyFromMap(map2, Record12.class);

        assertEquals(expected, res);
    }

    @Test
    void test4_record_nulls() {
        var rec = new Record12(null, 2, null, 4, null);
        var mapper = LaMapperJ.autoMapper(Record12.class, Record12.class);
        var res = mapper.transform(rec);
        assertEquals(rec, res);
    }

    private Test1Pojo getMock() {
        var o = new Test1Pojo();
        o.field1 = "f1";
        o.assignField2("f2");
        o.setProp2("p2");
        o.prop3 = "p3";
        o.setProp4("p4");
        o.setProp5("p5");
        o.setWrong1("1");
        o.setWrong2(2);
        return o;
    }

    static <To> To copyFromMap(Map<String, Object> fr, Class<To> targetClass) {
        if (fr == null)
            return null;
        List<Pair<String, Function<Object, Object>>> fieldMappers = fr.entrySet().stream()
            .map(e -> new Pair<String, Function<Object, Object>>(e.getKey(), f -> e.getValue()))
            .toList();
        return LaMapper.Companion.getGlobal().copyFromJ(fr, Object.class, targetClass, fieldMappers);
    }
}
