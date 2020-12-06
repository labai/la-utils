package com.github.labai.deci.converter.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.labai.deci.Deci;

import java.io.IOException;

/**
 * @author Augustus
 * created on 2020.11.29
 *
 * 	ObjectMapper mapper = new ObjectMapper();
 *	mapper.registerModule(JacksonDeciConverters.deciTypeModule();
 *
 */
public class JacksonDeciRegister {

	public static Module deciTypeModule() {
		SimpleModule module = new SimpleModule();
	 // module.addSerializer(Deci.class, new JacksonDeciSerializer())
		module.addDeserializer(Deci.class, new JacksonDeciDeserializer());
		return module;
	}


    // Deci serializers/deserializers
	public static class JacksonDeciDeserializer extends StdDeserializer<Deci> {

		protected JacksonDeciDeserializer() {
			super(Deci.class);
		}

		@Override
		public Deci deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JsonProcessingException {
			return new Deci(jp.readValueAs(String.class));
		}
	}

	// should work standard toString
//	public static class JacksonDeciSerializer extends StdSerializer<Deci> {
//		public JacksonDeciSerializer(){
//			super(Deci.class);
//		}
//		@Override
//		public void serialize(Deci value, JsonGenerator gen, SerializerProvider sp) throws IOException, JsonProcessingException {
//			gen.writeString(value.toString());
//		}
//	}
}
