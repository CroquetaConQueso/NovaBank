package com.novabank.operacion.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.exception.RemoteConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
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

    @Test
    void depositarLlamaEndpointInternoYDevuelveCuentaActualizada() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/10/depositos"))
                .withRequestBody(equalToJson("""
                        {
                          "cantidad": 50.00
                        }
                        """, true, true))
                .willReturn(okJson(cuentaJson(10L, "ES91210000000000000001", "150.00"))));

        StepVerifier.create(client().depositar(10L, new CuentaOperacionRequestDTO(new BigDecimal("50.00"))))
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

        StepVerifier.create(client().retirar(10L, new CuentaOperacionRequestDTO(new BigDecimal("25.00"))))
                .assertNext(response -> assertThat(response.saldo()).isEqualByComparingTo("75.00"))
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/retiros")));
    }

    @Test
    void transferenciaUsaEndpointAtomicoYDevuelveCuentasActualizadas() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .withRequestBody(equalToJson("""
                        {
                          "operationId": "op-test",
                          "cuentaOrigenId": 10,
                          "cuentaDestinoId": 11,
                          "monto": 25.00,
                          "concepto": "Transferencia entre cuentas"
                        }
                        """, true, true))
                .willReturn(okJson("""
                        {
                          "operationId": "op-test",
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

        StepVerifier.create(client().aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-test",
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
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "INSUFFICIENT_BALANCE",
                                  "message": "Saldo insuficiente",
                                  "service": "cuenta-service"
                                }
                                """)));

        StepVerifier.create(client().retirar(10L, new CuentaOperacionRequestDTO(new BigDecimal("999.00"))))
                .expectError(RemoteValidationException.class)
                .verify();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/retiros")));
    }

    @Test
    void error404RemotoSeTraduceAResourceNotFound() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/99/depositos"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "RESOURCE_NOT_FOUND",
                                  "message": "No existe ninguna cuenta con id 99",
                                  "service": "cuenta-service"
                                }
                                """)));

        StepVerifier.create(client().depositar(99L, new CuentaOperacionRequestDTO(new BigDecimal("10.00"))))
                .expectError(RemoteResourceNotFoundException.class)
                .verify();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/99/depositos")));
    }

    @Test
    void error400RemotoSeTraduceAValidationException() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "VALIDATION_ERROR",
                                  "message": "La cuenta origen y destino deben ser diferentes",
                                  "service": "cuenta-service"
                                }
                                """)));

        StepVerifier.create(client().aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-test",
                        10L,
                        10L,
                        new BigDecimal("10.00"),
                        "Transferencia"
                )))
                .expectError(RemoteValidationException.class)
                .verify();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/aplicar-movimientos")));
    }

    @Test
    void error409RemotoSeTraduceAConflictException() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "IDEMPOTENCY_CONFLICT",
                                  "message": "Operacion duplicada con body distinto",
                                  "service": "cuenta-service"
                                }
                                """)));

        StepVerifier.create(client().aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-test",
                        10L,
                        11L,
                        new BigDecimal("10.00"),
                        "Transferencia"
                )))
                .expectError(RemoteConflictException.class)
                .verify();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/aplicar-movimientos")));
    }

    @Test
    void error500RemotoSeTraduceAServicioNoDisponible() {
        wireMock.stubFor(post(urlEqualTo("/internal/cuentas/aplicar-movimientos"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "INTERNAL_ERROR",
                                  "message": "Error inesperado",
                                  "service": "cuenta-service"
                                }
                                """)));

        StepVerifier.create(client().aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        "op-test",
                        10L,
                        11L,
                        new BigDecimal("10.00"),
                        "Transferencia"
                )))
                .expectError(RemoteServiceException.class)
                .verify();

        wireMock.verify(postRequestedFor(urlEqualTo("/internal/cuentas/aplicar-movimientos")));
    }

    @Test
    void falloTecnicoSeTraduceAServicioNoDisponible() {
        CuentaServiceClient cuentaServiceClient = new CuentaServiceClient(WebClient.builder(), "http://localhost:1");

        StepVerifier.create(cuentaServiceClient.depositar(10L, new CuentaOperacionRequestDTO(new BigDecimal("10.00"))))
                .expectError(RemoteServiceException.class)
                .verify();
    }

    private CuentaServiceClient client() {
        return new CuentaServiceClient(WebClient.builder(), wireMock.getRuntimeInfo().getHttpBaseUrl());
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
}
