package com.novabank.operacion.streams.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JacksonJsonSerde<T> implements Serde<T> {

    private final Serializer<T> serializer;
    private final Deserializer<T> deserializer;

    public JacksonJsonSerde(ObjectMapper objectMapper, Class<T> type) {
        ObjectMapper mapper = objectMapper.copy().findAndRegisterModules();
        this.serializer = (topic, data) -> {
            if (data == null) {
                return null;
            }
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception ex) {
                throw new SerializationException("No se pudo serializar JSON para " + type.getSimpleName(), ex);
            }
        };
        this.deserializer = (topic, data) -> {
            if (data == null || data.length == 0) {
                return null;
            }
            try {
                return mapper.readValue(data, type);
            } catch (Exception ex) {
                throw new SerializationException("No se pudo deserializar JSON para " + type.getSimpleName(), ex);
            }
        };
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }
}
