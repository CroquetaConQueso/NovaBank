package com.novabank.operacion.client;

import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
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
    void depositarLlamaEndpointInternoYDevuelveMovimiento() {
        stubFor(post(urlEqualTo("/internal/cuentas/10/depositos"))
                .withRequestBody(equalToJson("""
                        {
                          "cantidad": 50.00
                        }
                        """, true, true))
                .willReturn(okJson(movimientoJson("DEPOSITO"))));

        MovimientoResponseDTO response = cuentaServiceClient.depositar(
                10L,
                new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
        );

        assertThat(response.tipo()).isEqualTo("DEPOSITO");
        assertThat(response.cantidad()).isEqualByComparingTo("50.00");
        verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/depositos")));
    }

    @Test
    void retirarLlamaEndpointInternoYDevuelveMovimiento() {
        stubFor(post(urlEqualTo("/internal/cuentas/10/retiros"))
                .willReturn(okJson(movimientoJson("RETIRO"))));

        MovimientoResponseDTO response = cuentaServiceClient.retirar(
                10L,
                new CuentaOperacionRequestDTO(new BigDecimal("25.00"))
        );

        assertThat(response.tipo()).isEqualTo("RETIRO");
        verify(postRequestedFor(urlEqualTo("/internal/cuentas/10/retiros")));
    }

    @Test
    void transferirUsaEndpointInternoUnicoYDevuelveDosMovimientos() {
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
                            "id": 1,
                            "cuentaId": 10,
                            "numeroCuenta": "ES91210000000000000001",
                            "tipo": "TRANSFERENCIA_SALIENTE",
                            "cantidad": 25.00,
                            "fecha": "2026-01-15T10:30:00"
                          },
                          {
                            "id": 2,
                            "cuentaId": 11,
                            "numeroCuenta": "ES91210000000000000002",
                            "tipo": "TRANSFERENCIA_ENTRANTE",
                            "cantidad": 25.00,
                            "fecha": "2026-01-15T10:30:01"
                          }
                        ]
                        """)));

        List<MovimientoResponseDTO> response = cuentaServiceClient.transferir(
                new TransferenciaInternaRequestDTO(10L, 11L, new BigDecimal("25.00"))
        );

        assertThat(response).hasSize(2);
        assertThat(response.get(0).tipo()).isEqualTo("TRANSFERENCIA_SALIENTE");
        assertThat(response.get(1).tipo()).isEqualTo("TRANSFERENCIA_ENTRANTE");
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

    private String movimientoJson(String tipo) {
        return """
                {
                  "id": 1,
                  "cuentaId": 10,
                  "numeroCuenta": "ES91210000000000000001",
                  "tipo": "%s",
                  "cantidad": 50.00,
                  "fecha": "2026-01-15T10:30:00"
                }
                """.formatted(tipo);
    }
}
