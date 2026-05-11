package com.novabank.operacion.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(
                WebClient.builder(),
                wireMock.getRuntimeInfo().getHttpBaseUrl(),
                Duration.ofMillis(100)
        );
    }

    @Test
    void devuelveTasaCuandoProveedorRespondeOk() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "USD",
                          "to": "EUR",
                          "tasa": 0.92,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerTasa("usd", "eur"))
                .assertNext(tasa -> assertThat(tasa).isEqualByComparingTo("0.92"))
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/exchange-rate?from=USD&to=EUR")));
    }

    @Test
    void fallaSiLaTasaEsCeroONegativa() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "USD",
                          "to": "EUR",
                          "tasa": 0,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerTasa("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void fallaSiProveedorDevuelveError5xx() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "INTERNAL_ERROR",
                                  "message": "Error remoto"
                                }
                                """)));

        StepVerifier.create(exchangeRateService.obtenerTasa("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void fallaSiProveedorNoRespondeATiempo() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(aResponse()
                        .withFixedDelay(250)
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "from": "USD",
                                  "to": "EUR",
                                  "tasa": 0.92,
                                  "timestamp": "2026-05-11T10:00:00Z"
                                }
                                """)));

        StepVerifier.create(exchangeRateService.obtenerTasa("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }
}
