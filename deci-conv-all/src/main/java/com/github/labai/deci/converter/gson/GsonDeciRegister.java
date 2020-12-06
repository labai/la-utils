package com.github.labai.deci.converter.gson;

import com.github.labai.deci.Deci;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * @author Augustus
 * created on 2020.11.29
 *
 *  gson = GsonBuilder()
 *      .registerTypeAdapter(Deci::class.java, GsonDeciRegister.deciTypeAdapter())
 *      .create();
 *
 */
public class GsonDeciRegister {

    public static DeciTypeAdapter deciTypeAdapter() {
        return new DeciTypeAdapter();
    }

    public static class DeciTypeAdapter implements JsonSerializer<Deci>, JsonDeserializer<Deci> {

        @Override
        public JsonElement serialize(Deci deci, Type typeOfT, JsonSerializationContext context) {
            return new JsonPrimitive(deci.toBigDecimal());
        }

        @Override
        public Deci deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String str = json.getAsString();
            return (str == null || str.isEmpty()) ? null : new Deci(str);
        }
    }

}
