package f18a14c09s.integration.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import f18a14c09s.util.StringObjectMap;

import java.io.*;
import java.util.Map;

public class JSONAdapter {
    private ObjectMapper defaultJsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public JSONAdapter() {
        defaultJsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public <R> R readValueFromResource(String path, Class<R> returnType) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            return defaultJsonMapper.readValue(stream, returnType);
        }
    }

    public String writeValueAsString(Object o) throws JsonProcessingException {
        return defaultJsonMapper.writeValueAsString(o);
    }

    public void writeValue(Writer writer, Object o) throws IOException {
        defaultJsonMapper.writeValue(writer, o);
    }

    public <T> T readValue(byte[] bytes, Class<T> clazz) throws IOException {
        return defaultJsonMapper.readValue(bytes, clazz);
    }

    public <T> T readValue(String json, Class<T> clazz) throws IOException {
        return defaultJsonMapper.readValue(json, clazz);
    }

    public <T> T readValue(Reader reader, Class<T> clazz) throws IOException {
        return defaultJsonMapper.readValue(reader, clazz);
    }

    public <T> T readValue(File file, Class<T> clazz) throws IOException {
        return defaultJsonMapper.readValue(file, clazz);
    }

    public Map<String, Object> readObjectMap(InputStream inputStream) throws IOException {
        return defaultJsonMapper.readValue(inputStream, StringObjectMap.class);
    }
}
