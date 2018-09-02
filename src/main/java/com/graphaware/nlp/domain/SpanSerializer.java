package com.graphaware.nlp.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;

import java.io.IOException;

public class SpanSerializer extends JsonSerializer<Span> {

    @Override
    public void serialize(Span span, org.codehaus.jackson.JsonGenerator jsonGenerator, org.codehaus.jackson.map.SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("first", String.valueOf(span.first()));
        jsonGenerator.writeStringField("second", String.valueOf(span.second()));
        jsonGenerator.writeEndObject();
    }
}
