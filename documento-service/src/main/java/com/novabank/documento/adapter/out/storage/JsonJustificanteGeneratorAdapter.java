package com.novabank.documento.adapter.out.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.documento.application.exception.DocumentoStorageException;
import com.novabank.documento.application.port.in.GenerarJustificanteOperacionCommand;
import com.novabank.documento.application.port.out.GeneratedJustificante;
import com.novabank.documento.application.port.out.JustificanteGeneratorPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JsonJustificanteGeneratorAdapter implements JustificanteGeneratorPort {

    public static final String CONTENT_TYPE = "application/json";

    private final ObjectMapper objectMapper;

    public JsonJustificanteGeneratorAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<GeneratedJustificante> generar(GenerarJustificanteOperacionCommand command) {
        return Mono.fromCallable(() -> {
            try {
                JustificanteJson json = new JustificanteJson(
                        command.operationId(),
                        command.correlationId(),
                        command.tipoOperacion(),
                        command.cuentaOrigenId(),
                        command.cuentaDestinoId(),
                        command.importe(),
                        command.moneda(),
                        command.occurredAt(),
                        Instant.now()
                );
                return new GeneratedJustificante(
                        objectMapper.writeValueAsString(json).getBytes(StandardCharsets.UTF_8),
                        CONTENT_TYPE
                );
            } catch (JsonProcessingException error) {
                throw new DocumentoStorageException("No se pudo generar justificante JSON", error);
            }
        });
    }
}
