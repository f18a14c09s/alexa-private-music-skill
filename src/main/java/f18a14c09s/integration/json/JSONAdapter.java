package f18a14c09s.integration.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class JSONAdapter {
    private ObjectMapper defaultJsonMapper = new ObjectMapper();

    public <R> R readValueFromResource(String path, Class<R> returnType) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            return defaultJsonMapper.readValue(stream, returnType);
        }
    }

    public String writeValueAsString(Object o) throws JsonProcessingException {
        return defaultJsonMapper.writeValueAsString(o);
    }

    public <T> T readValue(byte[] bytes, Class<T> clazz) throws IOException {
        return defaultJsonMapper.readValue(bytes, clazz);
    }
}
