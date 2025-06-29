package targetbuilder.perf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

/*
 * @author Augustus
 * created on 2025-06-25
 *
 *
 * Test for 100(times) x 2000(size)
 *
 * java temurin-jdk-17.0.15:
 * gson:552 jackson:505 custom:509
 *
 * java temurin-jdk-21.0.6:
 * gson:717 jackson:613 custom:495
 *
 * Resolution:
 * TargetBuilder gives no visible performance gain to compare with standard Jackson parser
 * (in some java versions may be small difference)
 *
 */
public class JsonParsePerfTest {
    private final static Gson gson = new Gson();
    private final ObjectMapper stdMapper = getStdMapper();
    private final ObjectMapper customMapper = getCustomMapper();

    public static class Wrapper {
        public List<Pojo2> pojoList;
    }

    @Test
    void test_json_perf() {
        var json = prepareJson(2000);

        // warmup
        readGson(1, json);
        readJacksonStd(1, json);
        readJacksonCustom(1, json);

        runAll(100, json);
    }

    private void runAll(int count, String json) {
        readGson(count, json);
        readJacksonStd(count, json);
        readJacksonCustom(count, json);
    }

    private static String prepareJson(int count) {
        // var pojo = createPojoSample();
        var list = new ArrayList<Pojo2>();
        for (int i = 0; i < count; i++) {
            list.add(createPojoSample(i));
        }
        Wrapper wrapper = new Wrapper();
        wrapper.pojoList = list;
        var json = gson.toJson(wrapper);
        return json;
    }

    private long readGson(int count, String json) {
        long startTs = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            Wrapper res = gson.fromJson(json, Wrapper.class);
        }
        out.println("c:" + count + " gson:" + (System.currentTimeMillis() - startTs));
        return System.currentTimeMillis() - startTs;
    }

    private long readJacksonStd(int count, String json) {
        long startTs = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            try {
                Wrapper res = stdMapper.readValue(json, Wrapper.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        out.println("c:" + count + " jackson:" + (System.currentTimeMillis() - startTs));
        return System.currentTimeMillis() - startTs;
    }

    private long readJacksonCustom(int count, String json) {
        long startTs = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            try {
                Wrapper res = customMapper.readValue(json, Wrapper.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        out.println("c:" + count + " custom:" + (System.currentTimeMillis() - startTs));
        return System.currentTimeMillis() - startTs;
    }

    private static Pojo2 createPojoSample(int i) {
        var pojo = new Pojo2();
        pojo.name = "Vardas " + i;
        pojo.address = "Vilnius " + i;
        pojo.age = 18;
        pojo.a00 = "a00 " + i;
        pojo.a01 = "a01 " + i;
        pojo.a02 = "a02 " + i;
        pojo.a03 = "a03 " + i;
        pojo.a04 = "a04 " + i;
        pojo.a05 = "a05 " + i;
        pojo.a06 = "a06 " + i;
        pojo.a07 = "a07 " + i;
        pojo.a08 = "a08 " + i;
        pojo.a09 = "a09 " + i;
        pojo.a10 = "a10 " + i;
        pojo.a11 = "a11 " + i;
        pojo.a12 = "a12 " + i;
        pojo.a13 = "a13 " + i;
        pojo.a14 = "a14 " + i;
        pojo.a15 = "a15 " + i;
        pojo.a16 = "a16 " + i;
        pojo.a17 = "a17 " + i;
        pojo.a18 = "a18 " + i;
        pojo.a19 = "a19 " + i;
        return pojo;
    }

    public static ObjectMapper getStdMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        return mapper;
    }

    public static ObjectMapper getCustomMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pojo2.class, new Pojo2Deserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
