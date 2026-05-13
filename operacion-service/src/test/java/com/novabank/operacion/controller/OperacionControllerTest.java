package com.novabank.operacion.controller;

import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaDivisaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.exception.GlobalExceptionHandler;
import com.novabank.operacion.exception.PublicIdempotencyConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
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
    void depositoDevuelveOperacionRealizada() {
        when(operacionService.depositar(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(response("DEPOSITO")));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("DEPOSITO")
                .jsonPath("$.mensaje").isEqualTo("Operacion realizada correctamente");
    }

    @Test
    void retiroSinCabecerasEspecialesDevuelveOperacionRealizada() {
        when(operacionService.retirar(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(response("RETIRO")));

        webTestClient.post()
                .uri("/api/operaciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("RETIRO");
    }

    @Test
    void transferenciaUsaRutaEsperada() {
        when(operacionService.transferir(any(TransferenciaRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(response("TRANSFERENCIA")));

        webTestClient.post()
                .uri("/api/operaciones/transferencia")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("TRANSFERENCIA");
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
    void cuentaServiceNoDisponibleDevuelve503Controlado() {
        when(operacionService.depositar(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.error(new RemoteServiceException("cuenta-service no esta disponible")));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUENTA_SERVICE_UNAVAILABLE")
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
    void depositoConCuentaNoEncontradaDevuelve404() {
        when(operacionService.depositar(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.error(new RemoteResourceNotFoundException("Cuenta no encontrada")));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(99L, new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void retiroConSaldoInsuficienteRemotoDevuelve422() {
        when(operacionService.retirar(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.error(new RemoteValidationException("Saldo insuficiente")));

        webTestClient.post()
                .uri("/api/operaciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("100.00")))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("REMOTE_VALIDATION_ERROR");
    }

    @Test
    void depositoAceptaIdempotencyKey() {
        when(operacionService.depositar(any(OperacionRequestDTO.class), nullable(String.class)))
                .thenReturn(Mono.just(response("DEPOSITO")));

        webTestClient.post()
                .uri("/api/operaciones/deposito")
                .header("Idempotency-Key", "deposito-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OperacionRequestDTO(10L, new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tipoOperacion").isEqualTo("DEPOSITO");
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
