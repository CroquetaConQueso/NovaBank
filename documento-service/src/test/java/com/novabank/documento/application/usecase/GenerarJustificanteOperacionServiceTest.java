package com.novabank.documento.application.usecase;

import com.novabank.documento.application.port.in.GenerarJustificanteOperacionCommand;
import com.novabank.documento.application.port.out.DocumentoStoragePort;
import com.novabank.documento.application.port.out.DocumentoUrlTemporal;
import com.novabank.documento.application.port.out.GeneratedJustificante;
import com.novabank.documento.application.port.out.JustificanteGeneratorPort;
import com.novabank.documento.domain.model.DocumentoOperacion;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GenerarJustificanteOperacionServiceTest {

    @Test
    void generaJustificanteYLoGuardaConClavePorCuenta() {
        UUID operationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AtomicReference<DocumentoOperacion> guardado = new AtomicReference<>();
        GenerarJustificanteOperacionService service = new GenerarJustificanteOperacionService(
                command -> Mono.just(new GeneratedJustificante(
                        "{\"ok\":true}".getBytes(StandardCharsets.UTF_8),
                        "application/json"
                )),
                new DocumentoStoragePort() {
                    @Override
                    public Mono<DocumentoOperacion> guardar(DocumentoOperacion documento, byte[] contenido) {
                        guardado.set(documento);
                        return Mono.just(documento);
                    }

                    @Override
                    public Mono<DocumentoUrlTemporal> generarUrlTemporalDescarga(UUID operacionId) {
                        return Mono.empty();
                    }

                    @Override
                    public Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId) {
                        return Flux.empty();
                    }

                    @Override
                    public Mono<Void> eliminarPorOperacion(UUID operacionId) {
                        return Mono.empty();
                    }

                    @Override
                    public Mono<Boolean> existePorOperacion(UUID operacionId) {
                        return Mono.just(false);
                    }
                }
        );

        StepVerifier.create(service.generar(new GenerarJustificanteOperacionCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-07-08T10:15:00Z"),
                        operationId,
                        "TRANSFERENCIA",
                        10L,
                        11L,
                        10L,
                        99L,
                        new BigDecimal("25.00"),
                        "EUR"
                )))
                .assertNext(documento -> {
                    assertThat(documento.operacionId()).isEqualTo(operationId);
                    assertThat(documento.cuentaId()).isEqualTo(10L);
                    assertThat(documento.claveObjeto())
                            .isEqualTo("cuentas/10/operaciones/2026/07/11111111-1111-1111-1111-111111111111.json");
                    assertThat(documento.contentType()).isEqualTo("application/json");
                })
                .verifyComplete();

        assertThat(guardado.get()).isNotNull();
    }

    @Test
    void siElEventoNoTraeCuentaPrincipalNoGuardaJustificante() {
        CapturingStoragePort storagePort = new CapturingStoragePort();
        GenerarJustificanteOperacionService service = new GenerarJustificanteOperacionService(
                command -> Mono.just(new GeneratedJustificante(new byte[]{1}, "application/json")),
                storagePort
        );

        StepVerifier.create(service.generar(new GenerarJustificanteOperacionCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-07-08T10:15:00Z"),
                        UUID.randomUUID(),
                        "DEPOSITO",
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("25.00"),
                        "EUR"
                )))
                .expectErrorMessage("cuentaIdPrincipal es obligatorio para generar justificante")
                .verify();

        assertThat(storagePort.guardado).isFalse();
    }

    private static class CapturingStoragePort implements DocumentoStoragePort {
        private boolean guardado;

        @Override
        public Mono<DocumentoOperacion> guardar(DocumentoOperacion documento, byte[] contenido) {
            guardado = true;
            return Mono.just(documento);
        }

        @Override
        public Mono<DocumentoUrlTemporal> generarUrlTemporalDescarga(UUID operacionId) {
            return Mono.empty();
        }

        @Override
        public Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId) {
            return Flux.empty();
        }

        @Override
        public Mono<Void> eliminarPorOperacion(UUID operacionId) {
            return Mono.empty();
        }

        @Override
        public Mono<Boolean> existePorOperacion(UUID operacionId) {
            return Mono.just(false);
        }
    }
}
