package com.novabank.operacion.client;

import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.openfeign.client.config.cuenta-service.url=http://localhost:${wiremock.server.port}",
        "spring.cloud.openfeign.circuitbreaker.enabled=false"
})
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class CuentaServiceClientContractTest {

    @Autowired
    private CuentaServiceClient cuentaServiceClient;

    @Test
    void depositarLlamaEndpointInternoYDevuelveCuentaActualizada() {
        stubFor(post(urlEqualTo("/internal/cuentas/10/depositos"))
                .withRequestBody(equalToJson("""
                        {
                          "cantidad": 50.00
                        }
                        """, true, true))
                .willReturn(okJson(cuentaJson(10L, "ES91210000000000000001", "150.00"))));

        CuentaResponseDTO response = cuentaServiceClient.depositar(
                10L,
                new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.saldo()).isEqualByComparingTo("150.00");
        verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/depositos")));
    }

    @Test
    void retirarLlamaEndpointInternoYDevuelveCuentaActualizada() {
        stubFor(post(urlEqualTo("/internal/cuentas/10/retiros"))
                .willReturn(okJson(cuentaJson(10L, "ES91210000000000000001", "75.00"))));

        CuentaResponseDTO response = cuentaServiceClient.retirar(
                10L,
                new CuentaOperacionRequestDTO(new BigDecimal("25.00"))
        );

        assertThat(response.saldo()).isEqualByComparingTo("75.00");
        verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/retiros")));
    }

    @Test
    void transferirUsaEndpointInternoUnicoYDevuelveDosCuentasActualizadas() {
        stubFor(post(urlEqualTo("/internal/cuentas/transferencias"))
                .withRequestBody(equalToJson("""
                        {
                          "cuentaOrigenId": 10,
                          "cuentaDestinoId": 11,
                          "cantidad": 25.00
                        }
                        """, true, true))
                .willReturn(okJson("""
                        [
                          {
                            "id": 10,
                            "numeroCuenta": "ES91210000000000000001",
                            "clienteId": 1,
                            "saldo": 75.00,
                            "fechaCreacion": "2026-01-15T10:30:00"
                          },
                          {
                            "id": 11,
                            "numeroCuenta": "ES91210000000000000002",
                            "clienteId": 2,
                            "saldo": 125.00,
                            "fechaCreacion": "2026-01-15T10:30:01"
                          }
                        ]
                        """)));

        List<CuentaResponseDTO> response = cuentaServiceClient.transferir(
                new TransferenciaInternaRequestDTO(10L, 11L, new BigDecimal("25.00"))
        );

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(10L);
        assertThat(response.get(1).id()).isEqualTo(11L);
        verify(postRequestedFor(urlEqualTo("/internal/cuentas/transferencias")));
    }

    @Test
    void error422RemotoSeTraduceAValidationException() {
        stubFor(post(urlEqualTo("/internal/cuentas/10/retiros"))
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

        assertThatThrownBy(() -> cuentaServiceClient.retirar(
                10L,
                new CuentaOperacionRequestDTO(new BigDecimal("999.00"))
        ))
                .isInstanceOf(RemoteValidationException.class)
                .hasMessageContaining("cuenta-service");

        verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/retiros")));
    }

    @Test
    void error404RemotoSeTraduceAResourceNotFound() {
        stubFor(post(urlEqualTo("/internal/cuentas/99/depositos"))
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

        assertThatThrownBy(() -> cuentaServiceClient.depositar(
                99L,
                new CuentaOperacionRequestDTO(new BigDecimal("10.00"))
        ))
                .isInstanceOf(RemoteResourceNotFoundException.class)
                .hasMessageContaining("cuenta");

        verify(postRequestedFor(urlEqualTo("/internal/cuentas/99/depositos")));
    }

    @Test
    void error400RemotoSeTraduceAValidationException() {
        stubFor(post(urlEqualTo("/internal/cuentas/transferencias"))
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

        assertThatThrownBy(() -> cuentaServiceClient.transferir(
                new TransferenciaInternaRequestDTO(10L, 10L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(RemoteValidationException.class)
                .hasMessageContaining("cuenta-service");

        verify(postRequestedFor(urlEqualTo("/internal/cuentas/transferencias")));
    }

    @Test
    void error500RemotoSeTraduceAServicioNoDisponible() {
        stubFor(post(urlEqualTo("/internal/cuentas/transferencias"))
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

        assertThatThrownBy(() -> cuentaServiceClient.transferir(
                new TransferenciaInternaRequestDTO(10L, 11L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cuenta-service");

        verify(postRequestedFor(urlEqualTo("/internal/cuentas/transferencias")));
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
