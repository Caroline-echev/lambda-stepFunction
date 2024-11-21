package com.credibanco.lambda_onboarding.infraestructure.config;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public class ByteBufferAdapter implements JsonSerializer<ByteBuffer>, JsonDeserializer<ByteBuffer> {

    @Override
    public JsonElement serialize(ByteBuffer src, Type typeOfSrc, JsonSerializationContext context) {

        byte[] bytes = new byte[src.remaining()];
        src.duplicate().get(bytes);
        return new JsonPrimitive(java.util.Base64.getEncoder().encodeToString(bytes));
    }

    @Override
    public ByteBuffer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        byte[] bytes = java.util.Base64.getDecoder().decode(json.getAsString());
        return ByteBuffer.wrap(bytes);
    }
}
