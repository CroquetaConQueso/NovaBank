package com.novabank.operacion.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.exception.RemoteConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class CuentaServiceClientContractTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private CuentaServiceClient cuentaServiceClient;

    @BeforeEach
    void setUp() {
        cuentaServiceClient = new CuentaServiceClient(WebClient.builder(), wireMock.getRuntimeInfo().getHttpBaseUrl());
    }

    @Test
    void depositarLlamaEndpointInternoYDevuelveCuentaActualizada() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/10/depositos"))
                .withRequestBody(equalToJson("""
                        {
                          "cantidad": 50.00
                        }
                        """, true, true))
                .willReturn(okJson(cuentaJson(10L, "ES91210000000000000001", "150.00"))));

        StepVerifier.create(cuentaServiceClient.depositar(
                        10L,
                        new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
                ))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(10L);
                    assertThat(response.saldo()).isEqualByComparingTo("150.00");
                })
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/depositos")));
    }

    @Test
    void retirarLlamaEndpointInternoYDevuelveCuentaActualizada() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/10/retiros"))
                .willReturn(okJson(cuentaJson(10L, "ES91210000000000000001", "75.00"))));

        StepVerifier.create(cuentaServiceClient.retirar(
                        10L,
                        new CuentaOperacionRequestDTO(new BigDecimal("25.00"))
                ))
                .assertNext(response -> assertThat(response.saldo()).isEqualByComparingTo("75.00"))
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/retiros")));
    }

    @Test
    void aplicarMovimientoUsaEndpointAtomicoYDevuelveCuentasActualizadas() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .withRequestBody(equalToJson("""
                        {
                          "operationId": "op-1",
                          "cuentaOrigenId": 10,
                          "cuentaDestinoId": 11,
                          "monto": 25.00,
                          "concepto": "Transferencia entre cuentas"
                        }
                        """, true, true))
                .willReturn(okJson("""
                        {
                          "operationId": "op-1",
                          "estado": "COMPLETED",
                          "mensaje": "Operacion aplicada",
                          "cuentaOrigen": {
                            "id": 10,
                            "numeroCuenta": "ES91210000000000000001",
                            "clienteId": 1,
                            "saldo": 75.00,
                            "fechaCreacion": "2026-01-15T10:30:00"
                          },
                          "cuentaDestino": {
                            "id": 11,
                            "numeroCuenta": "ES91210000000000000002",
                            "clienteId": 2,
                            "saldo": 125.00,
                            "fechaCreacion": "2026-01-15T10:30:01"
                          }
                        }
                        """)));

        StepVerifier.create(cuentaServiceClient.aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-1",
                        10L,
                        11L,
                        new BigDecimal("25.00"),
                        "Transferencia entre cuentas"
                )))
                .assertNext(response -> {
                    assertThat(response.cuentaOrigen().id()).isEqualTo(10L);
                    assertThat(response.cuentaDestino().id()).isEqualTo(11L);
                })
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/aplicar-movimientos")));
    }

    @Test
    void error422RemotoSeTraduceAValidationException() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/10/retiros"))
                .willReturn(errorResponse(422, "INSUFFICIENT_BALANCE", "Saldo insuficiente")));

        StepVerifier.create(cuentaServiceClient.retirar(
                        10L,
                        new CuentaOperacionRequestDTO(new BigDecimal("999.00"))
                ))
                .expectError(RemoteValidationException.class)
                .verify();
    }

    @Test
    void error404RemotoSeTraduceAResourceNotFound() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/99/depositos"))
                .willReturn(errorResponse(404, "RESOURCE_NOT_FOUND", "No existe ninguna cuenta con id 99")));

        StepVerifier.create(cuentaServiceClient.depositar(
                        99L,
                        new CuentaOperacionRequestDTO(new BigDecimal("10.00"))
                ))
                .expectError(RemoteResourceNotFoundException.class)
                .verify();
    }

    @Test
    void error409RemotoSeTraduceAConflictException() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .willReturn(errorResponse(409, "IDEMPOTENCY_CONFLICT", "La operacion ya existe con otro cuerpo")));

        StepVerifier.create(cuentaServiceClient.aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-1",
                        10L,
                        11L,
                        new BigDecimal("10.00"),
                        "Transferencia entre cuentas"
                )))
                .expectError(RemoteConflictException.class)
                .verify();
    }

    @Test
    void error400RemotoSeTraduceAValidationException() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .willReturn(errorResponse(400, "VALIDATION_ERROR", "La cuenta origen y destino deben ser diferentes")));

        StepVerifier.create(cuentaServiceClient.aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-1",
                        10L,
                        10L,
                        new BigDecimal("10.00"),
                        "Transferencia entre cuentas"
                )))
                .expectError(RemoteValidationException.class)
                .verify();
    }

    @Test
    void error500RemotoSeTraduceAServicioNoDisponible() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .willReturn(errorResponse(500, "INTERNAL_ERROR", "Error inesperado")));

        StepVerifier.create(cuentaServiceClient.aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-1",
                        10L,
                        11L,
                        new BigDecimal("10.00"),
                        "Transferencia entre cuentas"
                )))
                .expectError(RemoteServiceException.class)
                .verify();
    }

    private String cuentaJson(Long id, String numeroCuenta, String saldo) {
        return """
                {
                  "id": %d,
                  "numeroCuenta": "%s",
                  "clienteId": 1,
                  "saldo": %s,
                  "fechaCreacion": "2026-01-15T10:30:00"
                }
                """.formatted(id, numeroCuenta, saldo);
    }

    private ResponseDefinitionBuilder errorResponse(int status, String code, String message) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "code": "%s",
                          "message": "%s",
                          "service": "cuenta-service"
                        }
                        """.formatted(code, message));
    }
}
