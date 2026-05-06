package com.novabank.cuenta.client;

import com.novabank.cuenta.dto.ClienteResponseDTO;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.openfeign.client.config.cliente-service.url=http://localhost:${wiremock.server.port}",
        "spring.cloud.openfeign.circuitbreaker.enabled=false"
})
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class ClienteServiceClientContractTest {

    @Autowired
    private ClienteServiceClient clienteServiceClient;

    @Test
    void obtenerClienteDevuelveContratoEsperado() {
        stubFor(get(urlEqualTo("/api/clientes/7"))
                .willReturn(okJson("""
                        {
                          "id": 7,
                          "nombre": "Ana",
                          "apellidos": "Garcia",
                          "dni": "12345678Z",
                          "email": "ana@example.com",
                          "telefono": "600111222",
                          "fechaCreacion": "2026-01-15T10:30:00"
                        }
                        """)));

        ClienteResponseDTO response = clienteServiceClient.obtenerCliente(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.dni()).isEqualTo("12345678Z");
        assertThat(response.email()).isEqualTo("ana@example.com");
        verify(getRequestedFor(urlEqualTo("/api/clientes/7")));
    }

    @Test
    void obtenerCliente404SeTraduceAResourceNotFound() {
        stubFor(get(urlEqualTo("/api/clientes/99"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "RESOURCE_NOT_FOUND",
                                  "message": "No existe ningun cliente con id 99",
                                  "service": "cliente-service"
                                }
                                """)));

        assertThatThrownBy(() -> clienteServiceClient.obtenerCliente(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("cliente");

        verify(getRequestedFor(urlEqualTo("/api/clientes/99")));
    }

    @Test
    void obtenerCliente500SeTraduceAServicioNoDisponible() {
        stubFor(get(urlEqualTo("/api/clientes/7"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "INTERNAL_ERROR",
                                  "message": "Error inesperado",
                                  "service": "cliente-service"
                                }
                                """)));

        assertThatThrownBy(() -> clienteServiceClient.obtenerCliente(7L))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cliente-service");

        verify(getRequestedFor(urlEqualTo("/api/clientes/7")));
    }
}
