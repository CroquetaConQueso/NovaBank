package com.novabank.documento.application.usecase;

import com.novabank.documento.application.exception.DocumentoNotFoundException;
import com.novabank.documento.application.port.out.DocumentoStoragePort;
import com.novabank.documento.application.port.out.DocumentoUrlTemporal;
import com.novabank.documento.domain.model.DocumentoId;
import com.novabank.documento.domain.model.DocumentoOperacion;
import com.novabank.documento.domain.model.TipoDocumento;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

class DocumentoUseCaseServiceTest {

    @Test
    void generarUrlDescargaDevuelveUrlTemporal() {
        UUID operacionId = UUID.randomUUID();
        DocumentoUseCaseService service = new DocumentoUseCaseService(new DocumentoStoragePort() {
            @Override
            public Mono<DocumentoOperacion> guardar(DocumentoOperacion documento, byte[] contenido) {
                return Mono.just(documento);
            }

            @Override
            public Mono<DocumentoUrlTemporal> generarUrlTemporalDescarga(UUID id) {
                return Mono.just(new DocumentoUrlTemporal(
                        java.net.URI.create("http://localhost/documento.pdf"),
                        Instant.now().plusSeconds(900)
                ));
            }

            @Override
            public Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId) {
                return Flux.empty();
            }

            @Override
            public Mono<Void> eliminarPorOperacion(UUID id) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> existePorOperacion(UUID id) {
                return Mono.just(true);
            }
        });

        StepVerifier.create(service.generarUrlDescarga(operacionId))
                .assertNext(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.operacionId()).isEqualTo(operacionId);
                    org.assertj.core.api.Assertions.assertThat(result.url()).hasToString("http://localhost/documento.pdf");
                    org.assertj.core.api.Assertions.assertThat(result.expiraEn()).isAfter(Instant.now());
                })
                .verifyComplete();
    }

    @Test
    void listarPorCuentaDevuelveDocumentosDelStorage() {
        UUID operacionId = UUID.randomUUID();
        DocumentoOperacion documento = new DocumentoOperacion(
                DocumentoId.nuevo(),
                operacionId,
                10L,
                "documentos/" + operacionId + ".pdf",
                TipoDocumento.JUSTIFICANTE_OPERACION,
                "application/pdf",
                Instant.parse("2026-07-08T08:00:00Z")
        );

        DocumentoUseCaseService service = new DocumentoUseCaseService(new DocumentoStoragePort() {
            @Override
            public Mono<DocumentoOperacion> guardar(DocumentoOperacion documento, byte[] contenido) {
                return Mono.just(documento);
            }

            @Override
            public Mono<DocumentoUrlTemporal> generarUrlTemporalDescarga(UUID id) {
                return Mono.empty();
            }

            @Override
            public Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId) {
                return Flux.just(documento);
            }

            @Override
            public Mono<Void> eliminarPorOperacion(UUID id) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> existePorOperacion(UUID id) {
                return Mono.just(true);
            }
        });

        StepVerifier.create(service.listarPorCuenta(10L))
                .assertNext(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.operacionId()).isEqualTo(operacionId);
                    org.assertj.core.api.Assertions.assertThat(result.cuentaId()).isEqualTo(10L);
                    org.assertj.core.api.Assertions.assertThat(result.tipoDocumento()).isEqualTo(TipoDocumento.JUSTIFICANTE_OPERACION);
                })
                .verifyComplete();
    }

    @Test
    void eliminarDocumentoInexistenteDevuelveNotFound() {
        DocumentoUseCaseService service = new DocumentoUseCaseService(new DocumentoStoragePort() {
            @Override
            public Mono<DocumentoOperacion> guardar(DocumentoOperacion documento, byte[] contenido) {
                return Mono.just(documento);
            }

            @Override
            public Mono<DocumentoUrlTemporal> generarUrlTemporalDescarga(UUID id) {
                return Mono.empty();
            }

            @Override
            public Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId) {
                return Flux.empty();
            }

            @Override
            public Mono<Void> eliminarPorOperacion(UUID id) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> existePorOperacion(UUID id) {
                return Mono.just(false);
            }
        });

        StepVerifier.create(service.eliminarPorOperacion(UUID.randomUUID()))
                .expectError(DocumentoNotFoundException.class)
                .verify();
    }
}
