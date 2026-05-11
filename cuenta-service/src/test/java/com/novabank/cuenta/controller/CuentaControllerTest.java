package com.novabank.cuenta.controller;

import com.novabank.cuenta.dto.AplicarMovimientoRequestDTO;
import com.novabank.cuenta.dto.AplicarMovimientoResponseDTO;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.GlobalExceptionHandler;
import com.novabank.cuenta.exception.IdempotencyConflictException;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.service.CuentaMovimientoAtomicoService;
import com.novabank.cuenta.service.CuentaService;
import com.novabank.cuenta.service.MovimientoEventService;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest({CuentaController.class, InternalCuentaController.class})
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class CuentaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CuentaService cuentaService;

    @MockBean
    private CuentaMovimientoAtomicoService cuentaMovimientoAtomicoService;

    @MockBean
    private MovimientoEventService movimientoEventService;

    @Test
    void crearCuentaDevuelveCreatedYBody() {
        when(cuentaService.crearCuenta(any(CuentaCreateRequestDTO.class))).thenReturn(Mono.just(cuentaResponse()));

        webTestClient.post()
                .uri("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaCreateRequestDTO(1L))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.clienteId").isEqualTo(1);
    }

    @Test
    void listarCuentasPorClienteUsaRutaEsperada() {
        when(cuentaService.listarCuentasPorCliente(1L)).thenReturn(Flux.just(cuentaResponse()));

        webTestClient.get()
                .uri("/api/cuentas/cliente/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].numeroCuenta").isEqualTo("ES91210000000000000001");
    }

    @Test
    void obtenerCuentaPorIdDevuelveCuenta() {
        when(cuentaService.obtenerCuenta(10L)).thenReturn(Mono.just(cuentaResponse()));

        webTestClient.get()
                .uri("/api/cuentas/10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.numeroCuenta").isEqualTo("ES91210000000000000001");
    }

    @Test
    void obtenerCuentaPorNumeroDevuelveCuenta() {
        when(cuentaService.obtenerCuentaPorNumero("ES91210000000000000001")).thenReturn(Mono.just(cuentaResponse()));

        webTestClient.get()
                .uri("/api/cuentas/numero/ES91210000000000000001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(10);
    }

    @Test
    void consultarSaldoDevuelveSaldo() {
        when(cuentaService.consultarSaldo(10L))
                .thenReturn(Mono.just(new SaldoResponseDTO(10L, "ES91210000000000000001", new BigDecimal("50.00"))));

        webTestClient.get()
                .uri("/api/cuentas/10/saldo")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.saldo").isEqualTo(50.00);
    }

    @Test
    void depositarInternoDevuelveCuentaActualizada() {
        when(cuentaService.depositar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.just(cuentaResponse("100.00")));

        webTestClient.post()
                .uri("/internal/cuentas/10/depositos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaOperacionRequestDTO(new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.saldo").isEqualTo(100.00);
    }

    @Test
    void depositarCantidadDecimalMinimaDevuelveCuentaActualizada() {
        when(cuentaService.depositar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.just(cuentaResponse("50.01")));

        webTestClient.post()
                .uri("/internal/cuentas/10/depositos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaOperacionRequestDTO(new BigDecimal("0.01")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.saldo").isEqualTo(50.01);
    }

    @Test
    void retirarInternoConSaldoInsuficienteDevuelve422() {
        when(cuentaService.retirar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.error(new InsufficientBalanceException("Saldo insuficiente")));

        webTestClient.post()
                .uri("/internal/cuentas/10/retiros")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaOperacionRequestDTO(new BigDecimal("50.00")))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_BALANCE")
                .jsonPath("$.service").isEqualTo("cuenta-service");
    }

    @Test
    void transferirInternoDevuelveDosCuentasActualizadas() {
        when(cuentaService.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(Flux.just(
                        cuentaResponse(1L, "ES91210000000000000001", "25.00"),
                        cuentaResponse(2L, "ES91210000000000000002", "75.00")
                ));

        webTestClient.post()
                .uri("/internal/cuentas/transferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("25.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[1].id").isEqualTo(2);
    }

    @Test
    void cuentaNoEncontradaDevuelve404() {
        when(cuentaService.obtenerCuenta(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ninguna cuenta con id 99")));

        webTestClient.get()
                .uri("/api/cuentas/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void clienteServiceNoDisponibleDevuelve503Controlado() {
        when(cuentaService.crearCuenta(any(CuentaCreateRequestDTO.class)))
                .thenReturn(Mono.error(new RemoteServiceException("cliente-service no esta disponible")));

        webTestClient.post()
                .uri("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaCreateRequestDTO(1L))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CLIENTE_SERVICE_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("cuenta-service");
    }

    @Test
    void crearCuentaConClienteIdNuloDevuelve400() {
        webTestClient.post()
                .uri("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.clienteId").exists();
    }

    @Test
    void crearCuentaConClienteIdNegativoDevuelve400() {
        webTestClient.post()
                .uri("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaCreateRequestDTO(-1L))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.clienteId").exists();
    }

    @Test
    void crearCuentaConJsonMalformadoDevuelve400() {
        webTestClient.post()
                .uri("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void listarCuentasDeClienteInexistenteDevuelve404() {
        when(cuentaService.listarCuentasPorCliente(99L))
                .thenReturn(Flux.error(new ResourceNotFoundException("No existe ningun cliente con id 99")));

        webTestClient.get()
                .uri("/api/cuentas/cliente/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void obtenerCuentaPorNumeroInexistenteDevuelve404() {
        when(cuentaService.obtenerCuentaPorNumero("ES91210000000000000999"))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ninguna cuenta con numero ES91210000000000000999")));

        webTestClient.get()
                .uri("/api/cuentas/numero/ES91210000000000000999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void consultarSaldoCuentaInexistenteDevuelve404() {
        when(cuentaService.consultarSaldo(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ninguna cuenta con id 99")));

        webTestClient.get()
                .uri("/api/cuentas/99/saldo")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void depositarCantidadCeroDevuelve400() {
        webTestClient.post()
                .uri("/internal/cuentas/10/depositos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaOperacionRequestDTO(BigDecimal.ZERO))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void retirarCantidadNegativaDevuelve400() {
        webTestClient.post()
                .uri("/internal/cuentas/10/retiros")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CuentaOperacionRequestDTO(new BigDecimal("-1.00")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void transferirMismaCuentaDevuelve400() {
        when(cuentaService.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(Flux.error(new IllegalArgumentException("La cuenta origen y destino deben ser distintas")));

        webTestClient.post()
                .uri("/internal/cuentas/transferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaInternaRequestDTO(10L, 10L, new BigDecimal("5.00")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void transferirCuentaDestinoInexistenteDevuelve404() {
        when(cuentaService.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(Flux.error(new ResourceNotFoundException("No existe ninguna cuenta con id 99")));

        webTestClient.post()
                .uri("/internal/cuentas/transferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaInternaRequestDTO(10L, 99L, new BigDecimal("5.00")))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void transferirCantidadCeroDevuelve400() {
        webTestClient.post()
                .uri("/internal/cuentas/transferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferenciaInternaRequestDTO(10L, 11L, BigDecimal.ZERO))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void streamMovimientosDevuelveTextEventStream() {
        when(movimientoEventService.streamDeCuenta(10L))
                .thenReturn(Flux.just(new MovimientoEventDTO(
                        10L,
                        null,
                        "DEPOSITO",
                        new BigDecimal("25.00"),
                        new BigDecimal("75.00"),
                        "Deposito interno",
                        LocalDateTime.now(),
                        null
                )));

        webTestClient.get()
                .uri("/api/cuentas/10/movimientos/stream")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(MovimientoEventDTO.class)
                .hasSize(1);
    }

    @Test
    void aplicarMovimientoAtomicoDevuelveRespuestaInterna() {
        when(cuentaMovimientoAtomicoService.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.just(new AplicarMovimientoResponseDTO(
                        "op-1",
                        "COMPLETED",
                        "Operacion aplicada correctamente",
                        cuentaResponse(1L, "ES91210000000000000001", "75.00"),
                        cuentaResponse(2L, "ES91210000000000000002", "125.00")
                )));

        webTestClient.post()
                .uri("/internal/cuentas/aplicar-movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AplicarMovimientoRequestDTO(
                        "op-1",
                        1L,
                        2L,
                        new BigDecimal("25.00"),
                        "Transferencia interna"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.operationId").isEqualTo("op-1")
                .jsonPath("$.estado").isEqualTo("COMPLETED")
                .jsonPath("$.cuentaOrigen.id").isEqualTo(1)
                .jsonPath("$.cuentaDestino.id").isEqualTo(2);
    }

    @Test
    void aplicarMovimientoSinOperationIdDevuelve400() {
        webTestClient.post()
                .uri("/internal/cuentas/aplicar-movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AplicarMovimientoRequestDTO(
                        " ",
                        1L,
                        2L,
                        new BigDecimal("25.00"),
                        null
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.operationId").exists();
    }

    @Test
    void aplicarMovimientoConConflictoIdempotenteDevuelve409() {
        when(cuentaMovimientoAtomicoService.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.error(new IdempotencyConflictException(
                        "La operacion ya existe con una peticion diferente"
                )));

        webTestClient.post()
                .uri("/internal/cuentas/aplicar-movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AplicarMovimientoRequestDTO(
                        "op-1",
                        1L,
                        2L,
                        new BigDecimal("25.00"),
                        null
                ))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    private CuentaResponseDTO cuentaResponse() {
        return cuentaResponse("50.00");
    }

    private CuentaResponseDTO cuentaResponse(String saldo) {
        return cuentaResponse(10L, "ES91210000000000000001", saldo);
    }

    private CuentaResponseDTO cuentaResponse(Long id, String numeroCuenta, String saldo) {
        return new CuentaResponseDTO(
                id,
                numeroCuenta,
                1L,
                new BigDecimal(saldo),
                LocalDateTime.now()
        );
    }
}
