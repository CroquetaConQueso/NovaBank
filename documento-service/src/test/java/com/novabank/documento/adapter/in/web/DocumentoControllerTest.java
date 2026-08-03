package com.novabank.documento.adapter.in.web;

import com.novabank.documento.application.exception.DocumentoNotFoundException;
import com.novabank.documento.application.port.in.DocumentoResumenResult;
import com.novabank.documento.application.port.in.EliminarDocumentoOperacionUseCase;
import com.novabank.documento.application.port.in.GenerarUrlDescargaDocumentoUseCase;
import com.novabank.documento.application.port.in.ListarDocumentosCuentaUseCase;
import com.novabank.documento.application.port.in.UrlDescargaDocumentoResult;
import com.novabank.documento.domain.model.TipoDocumento;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(DocumentoController.class)
@Import(GlobalExceptionHandler.class)
class DocumentoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GenerarUrlDescargaDocumentoUseCase generarUrlDescargaDocumentoUseCase;

    @MockBean
    private ListarDocumentosCuentaUseCase listarDocumentosCuentaUseCase;

    @MockBean
    private EliminarDocumentoOperacionUseCase eliminarDocumentoOperacionUseCase;

    @Test
    void generarUrlDescargaDevuelveOk() {
        UUID operacionId = UUID.randomUUID();
        when(generarUrlDescargaDocumentoUseCase.generarUrlDescarga(eq(operacionId)))
                .thenReturn(Mono.just(new UrlDescargaDocumentoResult(
                        operacionId,
                        URI.create("http://localhost/documento.pdf"),
                        Instant.parse("2026-07-08T08:15:00Z")
                )));

        webTestClient.get()
                .uri("/api/documentos/operaciones/{operacionId}/url", operacionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.operacionId").isEqualTo(operacionId.toString())
                .jsonPath("$.url").isEqualTo("http://localhost/documento.pdf");
    }

    @Test
    void generarUrlDescargaInexistenteDevuelve404() {
        UUID operacionId = UUID.randomUUID();
        when(generarUrlDescargaDocumentoUseCase.generarUrlDescarga(eq(operacionId)))
                .thenReturn(Mono.error(new DocumentoNotFoundException("No existe documento")));

        webTestClient.get()
                .uri("/api/documentos/operaciones/{operacionId}/url", operacionId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("DOCUMENTO_NO_ENCONTRADO");
    }

    @Test
    void listarPorCuentaDevuelveDocumentos() {
        UUID documentoId = UUID.randomUUID();
        UUID operacionId = UUID.randomUUID();
        when(listarDocumentosCuentaUseCase.listarPorCuenta(eq(10L)))
                .thenReturn(Flux.just(new DocumentoResumenResult(
                        documentoId,
                        operacionId,
                        10L,
                        TipoDocumento.JUSTIFICANTE_OPERACION,
                        "application/pdf",
                        Instant.parse("2026-07-08T08:00:00Z")
                )));

        webTestClient.get()
                .uri("/api/documentos/cuentas/{cuentaId}", 10L)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].documentoId").isEqualTo(documentoId.toString())
                .jsonPath("$[0].operacionId").isEqualTo(operacionId.toString())
                .jsonPath("$[0].tipoDocumento").isEqualTo("JUSTIFICANTE_OPERACION");
    }

    @Test
    void eliminarDocumentoDevuelve204() {
        UUID operacionId = UUID.randomUUID();
        when(eliminarDocumentoOperacionUseCase.eliminarPorOperacion(eq(operacionId)))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/documentos/operaciones/{operacionId}", operacionId)
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(eliminarDocumentoOperacionUseCase).eliminarPorOperacion(operacionId);
    }
}
