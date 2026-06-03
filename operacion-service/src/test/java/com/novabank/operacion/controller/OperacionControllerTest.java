package com.novabank.operacion.controller;

import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionAceptadaResponseDTO;
import com.novabank.operacion.dto.OperacionEstadoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaAceptadaResponseDTO;
import com.novabank.operacion.dto.TransferenciaDivisaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.EventoNoPublicadoException;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.exception.GlobalExceptionHandler;
import com.novabank.operacion.exception.OperacionAsincronaNotFoundException;
import com.novabank.operacion.exception.PublicIdempotencyConflictException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.service.OperacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@WebFluxTest(OperacionController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class OperacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OperacionService operacionService;

    @Test
    void depositoDevuelveOperacionAceptada() {
        when(operacionService.solicitarDeposito(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(aceptada("DEPOSITO", 10L)));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("DEPOSITO")
                .jsonPath("$.estado").isEqualTo("SOLICITADA")
                .jsonPath("$.operationId").exists()
                .jsonPath("$.cuentaId").isEqualTo(10);
    }

    @Test
    void retiroSinCabecerasEspecialesDevuelveOperacionAceptada() {
        when(operacionService.solicitarRetirada(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(aceptada("RETIRADA", 10L)));

        webTestClient.post()
                .uri("/api/operaciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("RETIRADA")
                .jsonPath("$.estado").isEqualTo("SOLICITADA");
    }

    @Test
    void transferenciaUsaRutaEsperada() {
        when(operacionService.transferir(any(TransferenciaRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(transferenciaAceptada()));

        webTestClient.post()
                .uri("/api/operaciones/transferencia")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.operationId").exists()
                .jsonPath("$.estado").isEqualTo("SOLICITADA")
                .jsonPath("$.tipoOperacion").isEqualTo("TRANSFERENCIA")
                .jsonPath("$.cuentaOrigenId").isEqualTo(10)
                .jsonPath("$.cuentaDestinoId").isEqualTo(11)
                .jsonPath("$.moneda").isEqualTo("EUR");
    }

    @Test
    void transferenciaEnDivisaUsaRutaEsperada() {
        when(operacionService.transferirEnDivisa(any(TransferenciaDivisaRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(response("TRANSFERENCIA")));

        webTestClient.post()
                .uri("/api/operaciones/transferencias/divisa")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaDivisaRequestDTO(
                        10L,
                        11L,
                        new BigDecimal("100.00"),
                        "USD",
                        "EUR"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("TRANSFERENCIA");
    }

    @Test
    void transferenciaEnDivisaSinTipoCambioDevuelve503() {
        when(operacionService.transferirEnDivisa(any(TransferenciaDivisaRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.error(new ExchangeRateUnavailableException("No hay tasa fiable")));

        webTestClient.post()
                .uri("/api/operaciones/transferencias/divisa")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaDivisaRequestDTO(
                        10L,
                        11L,
                        new BigDecimal("100.00"),
                        "USD",
                        "EUR"
                ))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("EXCHANGE_RATE_UNAVAILABLE");
    }

    @Test
    void listarMovimientosUsaRutaDeOperacionService() {
        when(operacionService.listarMovimientos(10L, null, null))
                .thenReturn(Flux.just(movimiento("DEPOSITO")));

        webTestClient.get()
                .uri("/api/operaciones/cuentas/10/movimientos")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].tipo").isEqualTo("DEPOSITO");
    }

    @Test
    void consultarSagaDevuelveEstadoPersistido() {
        UUID operationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(operacionService.consultarOperacionAsincrona(operationId))
                .thenReturn(Mono.just(estado(operationId, "COMPLETADA")));

        webTestClient.get()
                .uri("/api/operaciones/sagas/{operationId}", operationId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.operationId").isEqualTo(operationId.toString())
                .jsonPath("$.estado").isEqualTo("COMPLETADA")
                .jsonPath("$.tipoOperacion").isEqualTo("DEPOSITO");
    }

    @Test
    void consultarSagaInexistenteDevuelve404() {
        UUID operationId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(operacionService.consultarOperacionAsincrona(operationId))
                .thenReturn(Mono.error(new OperacionAsincronaNotFoundException("No existe operacion asincrona")));

        webTestClient.get()
                .uri("/api/operaciones/sagas/{operationId}", operationId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void consultarSagaTransferenciaDevuelveEstadoPersistido() {
        UUID operationId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(operacionService.consultarOperacionAsincrona(operationId))
                .thenReturn(Mono.just(estadoTransferencia(operationId, "FALLIDA")));

        webTestClient.get()
                .uri("/api/operaciones/sagas/{operationId}", operationId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.operationId").isEqualTo(operationId.toString())
                .jsonPath("$.tipoOperacion").isEqualTo("TRANSFERENCIA")
                .jsonPath("$.estado").isEqualTo("FALLIDA")
                .jsonPath("$.cuentaOrigenId").isEqualTo(10)
                .jsonPath("$.cuentaDestinoId").isEqualTo(11)
                .jsonPath("$.motivoFallo").isEqualTo("Saldo insuficiente");
    }

    @Test
    void depositoConPublicacionFallidaDevuelve503Controlado() {
        when(operacionService.solicitarDeposito(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.error(new EventoNoPublicadoException("No se pudo publicar OperacionSolicitadaEvent")));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("KAFKA_EVENT_NOT_PUBLISHED")
                .jsonPath("$.service").isEqualTo("operacion-service");
    }

    @Test
    void depositoConBodyVacioDevuelve400() {
        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.cuentaId").exists()
                .jsonPath("$.fieldErrors.cantidad").exists();
    }

    @Test
    void depositoConJsonMalformadoDevuelve400() {
        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void depositoConCuentaIdNegativoDevuelve400() {
        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(-1L, new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void depositoConCantidadCeroDevuelve400() {
        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, BigDecimal.ZERO))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void retiroConCantidadNegativaDevuelve400() {
        webTestClient.post()
                .uri("/api/operaciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("-1.00")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void transferenciaConCuentaOrigenNulaDevuelve400() {
        webTestClient.post()
                .uri("/api/operaciones/transferencia")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaRequestDTO(null, 11L, new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.cuentaOrigenId").exists();
    }

    @Test
    void transferenciaConCantidadCeroDevuelve400() {
        webTestClient.post()
                .uri("/api/operaciones/transferencia")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaRequestDTO(10L, 11L, BigDecimal.ZERO))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void listarMovimientosSinResultadosDevuelveListaVacia() {
        when(operacionService.listarMovimientos(10L, null, null)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/operaciones/cuentas/10/movimientos")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").isEmpty();
    }

    @Test
    void listarMovimientosConCuentaIdInvalidoDevuelve400() {
        when(operacionService.listarMovimientos(0L, null, null))
                .thenReturn(Flux.error(new ValidationException("El id de cuenta debe ser positivo")));

        webTestClient.get()
                .uri("/api/operaciones/cuentas/0/movimientos")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void listarMovimientosConRangoInvalidoDevuelve400() {
        LocalDate inicio = LocalDate.of(2026, 1, 10);
        LocalDate fin = LocalDate.of(2026, 1, 1);
        when(operacionService.listarMovimientos(10L, inicio, fin))
                .thenReturn(Flux.error(new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/operaciones/cuentas/10/movimientos")
                        .queryParam("fechaInicio", "2026-01-10")
                        .queryParam("fechaFin", "2026-01-01")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void depositoAsincronoNoValidaCuentaEnOperacionService() {
        when(operacionService.solicitarDeposito(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(aceptada("DEPOSITO", 99L)));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(99L, new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.cuentaId").isEqualTo(99)
                .jsonPath("$.estado").isEqualTo("SOLICITADA");
    }

    @Test
    void retiroAsincronoDevuelveAceptadoSinValidarSaldoEnOperacionService() {
        when(operacionService.solicitarRetirada(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(aceptada("RETIRADA", 10L)));

        webTestClient.post()
                .uri("/api/operaciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("100.00")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("RETIRADA")
                .jsonPath("$.estado").isEqualTo("SOLICITADA");
    }

    @Test
    void depositoAceptaIdempotencyKey() {
        when(operacionService.solicitarDeposito(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(aceptada("DEPOSITO", 10L)));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .header("Idempotency-Key", "deposito-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("DEPOSITO")
                .jsonPath("$.estado").isEqualTo("SOLICITADA");
    }

    @Test
    void conflictoDeIdempotenciaDevuelve409() {
        when(operacionService.transferir(any(TransferenciaRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.error(new PublicIdempotencyConflictException(
                        "La clave de idempotencia ya existe con una peticion diferente"
                )));

        webTestClient.post()
                .uri("/api/operaciones/transferencia")
                .header("Idempotency-Key", "transferencia-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("PUBLIC_IDEMPOTENCY_CONFLICT");
    }

    private OperacionResponseDTO response(String tipoOperacion) {
        return new OperacionResponseDTO(
                tipoOperacion,
                "Operacion realizada correctamente",
                List.of(movimiento(tipoOperacion))
        );
    }

    private OperacionAceptadaResponseDTO aceptada(String tipoOperacion, Long cuentaId) {
        return new OperacionAceptadaResponseDTO(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "SOLICITADA",
                tipoOperacion + " solicitada para procesamiento asincrono",
                tipoOperacion,
                cuentaId,
                new BigDecimal("50.00")
        );
    }

    private TransferenciaAceptadaResponseDTO transferenciaAceptada() {
        return new TransferenciaAceptadaResponseDTO(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "SOLICITADA",
                "TRANSFERENCIA solicitada para procesamiento asincrono",
                "TRANSFERENCIA",
                10L,
                11L,
                new BigDecimal("50.00"),
                "EUR"
        );
    }

    private OperacionEstadoResponseDTO estado(UUID operationId, String estado) {
        LocalDateTime ahora = LocalDateTime.now();
        return new OperacionEstadoResponseDTO(
                operationId,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "DEPOSITO",
                estado,
                10L,
                null,
                10L,
                new BigDecimal("50.00"),
                "EUR",
                null,
                ahora,
                ahora
        );
    }

    private OperacionEstadoResponseDTO estadoTransferencia(UUID operationId, String estado) {
        LocalDateTime ahora = LocalDateTime.now();
        return new OperacionEstadoResponseDTO(
                operationId,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "TRANSFERENCIA",
                estado,
                10L,
                10L,
                11L,
                new BigDecimal("50.00"),
                "EUR",
                "Saldo insuficiente",
                ahora,
                ahora
        );
    }

    private MovimientoResponseDTO movimiento(String tipoOperacion) {
        return new MovimientoResponseDTO(
                1L,
                10L,
                "ES91210000000000000001",
                tipoOperacion,
                new BigDecimal("50.00"),
                LocalDateTime.now()
        );
    }
}
