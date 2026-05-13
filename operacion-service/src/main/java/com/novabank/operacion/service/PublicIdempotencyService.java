package com.novabank.operacion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.exception.PublicIdempotencyConflictException;
import com.novabank.operacion.model.EstadoOperacionPublicaIdempotente;
import com.novabank.operacion.model.OperacionPublicaIdempotente;
import com.novabank.operacion.repository.OperacionPublicaIdempotenteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

@Service
public class PublicIdempotencyService {

    private final OperacionPublicaIdempotenteRepository repository;
    private final ObjectMapper objectMapper;

    public PublicIdempotencyService(
            OperacionPublicaIdempotenteRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Mono<OperacionResponseDTO> execute(
            String idempotencyKey,
            String tipoOperacion,
            String requestHash,
            Supplier<Mono<OperacionResponseDTO>> operation
    ) {
        /*
         * Sin clave publica se mantiene compatibilidad; con clave se evita
         * ejecutar dos veces la misma operacion financiera.
         */
        String key = normalizarKey(idempotencyKey);
        if (key == null) {
            return Mono.defer(operation);
        }

        return repository.findByIdempotencyKey(key)
                .flatMap(existing -> resolverExistente(existing, requestHash))
                .switchIfEmpty(Mono.defer(() -> registrarYEjecutar(key, tipoOperacion, requestHash, operation)));
    }

    public String hash(String contenidoNormalizado) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenidoNormalizado.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash de idempotencia publica", ex);
        }
    }

    public String normalizarKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
    }

    private Mono<OperacionResponseDTO> registrarYEjecutar(
            String idempotencyKey,
            String tipoOperacion,
            String requestHash,
            Supplier<Mono<OperacionResponseDTO>> operation
    ) {
        return repository.insertProcessingIfAbsent(idempotencyKey, requestHash, tipoOperacion)
                .flatMap(inserted -> {
                    if (inserted == null || inserted == 0) {
                        return resolverColision(idempotencyKey, requestHash);
                    }
                    return repository.findByIdempotencyKey(idempotencyKey)
                            .switchIfEmpty(Mono.error(new PublicIdempotencyConflictException(
                                    "La operacion idempotente no esta disponible para reutilizacion"
                            )))
                            .flatMap(stored -> Mono.defer(operation)
                                    .flatMap(response -> completar(stored, response)));
                })
                .onErrorResume(
                        DataIntegrityViolationException.class,
                        ex -> resolverColision(idempotencyKey, requestHash)
                );
    }

    private Mono<OperacionResponseDTO> resolverColision(String idempotencyKey, String requestHash) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .switchIfEmpty(Mono.error(new PublicIdempotencyConflictException(
                        "La operacion idempotente no esta disponible para reutilizacion"
                )))
                .flatMap(existing -> resolverExistente(existing, requestHash));
    }

    private Mono<OperacionResponseDTO> resolverExistente(
            OperacionPublicaIdempotente existing,
            String requestHash
    ) {
        if (!existing.getRequestHash().equals(requestHash)) {
            return Mono.error(new PublicIdempotencyConflictException(
                    "La clave de idempotencia ya existe con una peticion diferente"
            ));
        }

        if (existing.getEstado() == EstadoOperacionPublicaIdempotente.COMPLETED) {
            return Mono.fromCallable(() -> objectMapper.readValue(existing.getResponseJson(), OperacionResponseDTO.class));
        }

        return Mono.error(new PublicIdempotencyConflictException(
                "La operacion idempotente ya esta registrada y aun no puede reutilizarse"
        ));
    }

    private Mono<OperacionResponseDTO> completar(
            OperacionPublicaIdempotente stored,
            OperacionResponseDTO response
    ) {
        return Mono.fromCallable(() -> serializar(response))
                .flatMap(json -> {
                    stored.marcarCompletada(json);
                    return repository.save(stored);
                })
                .thenReturn(response);
    }

    private String serializar(OperacionResponseDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la respuesta idempotente", ex);
        }
    }
}
