package targetbuilder.perf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.labai.utils.targetbuilder.ITargetBuilderStringFactory;
import com.github.labai.utils.targetbuilder.TargetBuilderJ;

import java.io.IOException;

//
// simple jackson deserializer - takes tokens and put them to builder
//
public class Pojo2Deserializer extends JsonDeserializer<Pojo2> {
    private final ITargetBuilderStringFactory<Pojo2> factory;

    public Pojo2Deserializer() {
        factory = TargetBuilderJ.fromStringsSource(Pojo2.class);
        System.out.println("Pojo2Deserializer create");
    }

    @Override
    public Pojo2 deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        var builder = factory.instance();

        // Expecting START_OBJECT
        if (parser.currentToken() == null) {
            parser.nextToken();
        }

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected start of object");
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();
            parser.nextToken(); // move to field value
            builder.add(fieldName, parser.getText());
        }

        return builder.build();
    }
}
