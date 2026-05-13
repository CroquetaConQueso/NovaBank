package com.novabank.operacion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.exception.PublicIdempotencyConflictException;
import com.novabank.operacion.model.EstadoOperacionPublicaIdempotente;
import com.novabank.operacion.model.OperacionPublicaIdempotente;
import com.novabank.operacion.repository.OperacionPublicaIdempotenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicIdempotencyServiceTest {

    private OperacionPublicaIdempotenteRepository repository;
    private ObjectMapper objectMapper;
    private PublicIdempotencyService service;

    @BeforeEach
    void setUp() {
        repository = mock(OperacionPublicaIdempotenteRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new PublicIdempotencyService(repository, objectMapper);
    }

    @Test
    void claveNuevaInsertaProcessingEjecutaYMarcaCompleted() {
        OperacionPublicaIdempotente processing = operacion("key-1", "hash-1", EstadoOperacionPublicaIdempotente.PROCESSING, null);
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(processing));
        when(repository.insertProcessingIfAbsent("key-1", "hash-1", "DEPOSITO"))
                .thenReturn(Mono.just(1));
        when(repository.save(any(OperacionPublicaIdempotente.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute(
                        "key-1",
                        "DEPOSITO",
                        "hash-1",
                        () -> Mono.just(new OperacionResponseDTO("DEPOSITO", "ok", List.of()))
                ))
                .assertNext(response -> assertThat(response.tipoOperacion()).isEqualTo("DEPOSITO"))
                .verifyComplete();

        verify(repository).insertProcessingIfAbsent("key-1", "hash-1", "DEPOSITO");
        verify(repository).save(any(OperacionPublicaIdempotente.class));
    }

    @Test
    void mismaClaveYHashCompletedDevuelveRespuestaPersistida() throws Exception {
        OperacionResponseDTO response = new OperacionResponseDTO("RETIRO", "ok", List.of());
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Mono.just(operacion(
                        "key-1",
                        "hash-1",
                        EstadoOperacionPublicaIdempotente.COMPLETED,
                        objectMapper.writeValueAsString(response)
                )));

        StepVerifier.create(service.execute(
                        "key-1",
                        "RETIRO",
                        "hash-1",
                        () -> Mono.error(new IllegalStateException("No debe ejecutar la operacion"))
                ))
                .assertNext(repeated -> assertThat(repeated.tipoOperacion()).isEqualTo("RETIRO"))
                .verifyComplete();

        verify(repository, never()).insertProcessingIfAbsent(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void mismaClaveYHashProcessingDevuelveConflicto() {
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Mono.just(operacion(
                        "key-1",
                        "hash-1",
                        EstadoOperacionPublicaIdempotente.PROCESSING,
                        null
                )));

        StepVerifier.create(service.execute(
                        "key-1",
                        "TRANSFERENCIA",
                        "hash-1",
                        () -> Mono.just(new OperacionResponseDTO("TRANSFERENCIA", "ok", List.of()))
                ))
                .expectError(PublicIdempotencyConflictException.class)
                .verify();
    }

    @Test
    void mismaClaveYHashDistintoDevuelveConflicto() {
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Mono.just(operacion(
                        "key-1",
                        "hash-original",
                        EstadoOperacionPublicaIdempotente.COMPLETED,
                        "{}"
                )));

        StepVerifier.create(service.execute(
                        "key-1",
                        "TRANSFERENCIA",
                        "hash-distinto",
                        () -> Mono.just(new OperacionResponseDTO("TRANSFERENCIA", "ok", List.of()))
                ))
                .expectError(PublicIdempotencyConflictException.class)
                .verify();
    }

    @Test
    void colisionDeInsertConsultaRegistroExistente() throws Exception {
        OperacionResponseDTO response = new OperacionResponseDTO("DEPOSITO", "ok", List.of());
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(operacion(
                        "key-1",
                        "hash-1",
                        EstadoOperacionPublicaIdempotente.COMPLETED,
                        objectMapper.writeValueAsString(response)
                )));
        when(repository.insertProcessingIfAbsent(eq("key-1"), eq("hash-1"), eq("DEPOSITO")))
                .thenReturn(Mono.just(0));

        StepVerifier.create(service.execute(
                        "key-1",
                        "DEPOSITO",
                        "hash-1",
                        () -> Mono.error(new IllegalStateException("No debe ejecutar la operacion"))
                ))
                .assertNext(repeated -> assertThat(repeated.tipoOperacion()).isEqualTo("DEPOSITO"))
                .verifyComplete();
    }

    private OperacionPublicaIdempotente operacion(
            String key,
            String hash,
            EstadoOperacionPublicaIdempotente estado,
            String responseJson
    ) {
        return OperacionPublicaIdempotente.builder()
                .id(1L)
                .idempotencyKey(key)
                .requestHash(hash)
                .tipoOperacion("TEST")
                .estado(estado)
                .responseJson(responseJson)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }
}
