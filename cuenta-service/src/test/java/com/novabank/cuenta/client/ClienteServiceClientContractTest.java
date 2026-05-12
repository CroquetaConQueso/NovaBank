package com.novabank.cuenta.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.novabank.cuenta.dto.ClienteResponseDTO;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.config.WebClientConfig;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class ClienteServiceClientContractTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void obtenerClienteDevuelveContratoEsperado() {
        wireMock.stubFor(get(urlEqualTo("/api/clientes/7"))
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

        ClienteServiceClient clienteServiceClient = client();

        StepVerifier.create(clienteServiceClient.obtenerCliente(7L))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(7L);
                    assertThat(response.dni()).isEqualTo("12345678Z");
                    assertThat(response.email()).isEqualTo("ana@example.com");
                })
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/clientes/7")));
    }

    @Test
    void propagaCorrelationIdAlConsultarCliente() {
        wireMock.stubFor(get(urlEqualTo("/api/clientes/7"))
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

        StepVerifier.create(client().obtenerCliente(7L)
                        .contextWrite(context -> context.put(CorrelationIdSupport.CONTEXT_KEY, "cid-cuenta-test")))
                .assertNext(response -> assertThat(response.id()).isEqualTo(7L))
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/clientes/7"))
                .withHeader(CorrelationIdSupport.HEADER_NAME, com.github.tomakehurst.wiremock.client.WireMock.equalTo("cid-cuenta-test")));
    }

    @Test
    void obtenerCliente404SeTraduceAResourceNotFound() {
        wireMock.stubFor(get(urlEqualTo("/api/clientes/99"))
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

        ClienteServiceClient clienteServiceClient = client();

        StepVerifier.create(clienteServiceClient.obtenerCliente(99L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                    assertThat(error).hasMessageContaining("cliente");
                })
                .verify();

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/clientes/99")));
    }

    @Test
    void obtenerCliente500SeTraduceAServicioNoDisponible() {
        wireMock.stubFor(get(urlEqualTo("/api/clientes/7"))
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

        ClienteServiceClient clienteServiceClient = client();

        StepVerifier.create(clienteServiceClient.obtenerCliente(7L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RemoteServiceException.class);
                    assertThat(error).hasMessageContaining("cliente-service");
                })
                .verify();

        wireMock.verify(2, getRequestedFor(urlEqualTo("/api/clientes/7")));
    }

    @Test
    void obtenerClienteConFalloTecnicoSeTraduceAServicioNoDisponible() {
        ClienteServiceClient clienteServiceClient = new ClienteServiceClient(WebClient.builder(), "http://localhost:1");

        StepVerifier.create(clienteServiceClient.obtenerCliente(7L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RemoteServiceException.class);
                    assertThat(error).hasMessageContaining("cliente-service");
                })
                .verify();
    }

    private ClienteServiceClient client() {
        return new ClienteServiceClient(new WebClientConfig().webClientBuilder(), wireMock.getRuntimeInfo().getHttpBaseUrl());
    }
}
