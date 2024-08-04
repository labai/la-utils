package jtest;

import com.github.labai.utils.mapper.LaMapperJ;
import jtest.StructuresInJava.Record12;
import jtest.StructuresInJava.Test1Pojo;
import org.junit.jupiter.api.Test;

import static com.github.labai.utils.mapper.LaMapperJ.mapFrom;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2023.11.22
 */
class LaMapperJTest {

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
}
