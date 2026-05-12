package com.novabank.operacion.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.config.WebClientConfig;
import com.novabank.operacion.tracing.CorrelationIdSupport;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

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
                new WebClientConfig().webClientBuilder(),
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
    void tasaValidaRemotaSeCacheaYPermiteFallbackTecnico() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=GBP&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "GBP",
                          "to": "EUR",
                          "tasa": 1.16,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerCotizacion("GBP", "EUR"))
                .assertNext(cotizacion -> {
                    assertThat(cotizacion.tasa()).isEqualByComparingTo("1.16");
                    assertThat(cotizacion.cacheada()).isFalse();
                })
                .verifyComplete();

        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=GBP&to=EUR"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(exchangeRateService.obtenerCotizacion("GBP", "EUR"))
                .assertNext(cotizacion -> {
                    assertThat(cotizacion.tasa()).isEqualByComparingTo("1.16");
                    assertThat(cotizacion.cacheada()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void falloTecnicoSinCacheVigenteMantieneErrorControlado() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=CHF&to=EUR"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(exchangeRateService.obtenerCotizacion("CHF", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void cacheVencidaNoPermiteContinuarConFalloTecnico() {
        AtomicLong nanos = new AtomicLong();
        Cache<ExchangeRateService.ExchangeRatePair, BigDecimal> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .ticker(nanos::get)
                .build();
        ExchangeRateService serviceConCacheControlado = new ExchangeRateService(
                new WebClientConfig().webClientBuilder(),
                wireMock.getRuntimeInfo().getHttpBaseUrl(),
                Duration.ofMillis(100),
                CircuitBreaker.ofDefaults("exchangeRateCacheExpiryTest"),
                Retry.ofDefaults("exchangeRateCacheExpiryTest"),
                cache
        );

        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=EUR&to=USD"))
                .willReturn(okJson("""
                        {
                          "from": "EUR",
                          "to": "USD",
                          "tasa": 1.08,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(serviceConCacheControlado.obtenerCotizacion("EUR", "USD"))
                .assertNext(cotizacion -> assertThat(cotizacion.tasa()).isEqualByComparingTo("1.08"))
                .verifyComplete();

        nanos.addAndGet(Duration.ofMinutes(6).toNanos());
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=EUR&to=USD"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(serviceConCacheControlado.obtenerCotizacion("EUR", "USD"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void errorFuncionalNoUsaCacheAunqueExistaTasaPrevia() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "USD",
                          "to": "EUR",
                          "tasa": 0.92,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerCotizacion("USD", "EUR"))
                .assertNext(cotizacion -> assertThat(cotizacion.tasa()).isEqualByComparingTo("0.92"))
                .verifyComplete();

        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "EXCHANGE_RATE_NOT_FOUND",
                                  "message": "Tasa no soportada"
                                }
                                """)));

        StepVerifier.create(exchangeRateService.obtenerCotizacion("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void propagaCorrelationIdHaciaExchangeRateService() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "USD",
                          "to": "EUR",
                          "tasa": 0.92,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerTasa("USD", "EUR")
                        .contextWrite(context -> context.put(CorrelationIdSupport.CONTEXT_KEY, "cid-rate-test")))
                .assertNext(tasa -> assertThat(tasa).isEqualByComparingTo("0.92"))
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .withHeader(CorrelationIdSupport.HEADER_NAME, com.github.tomakehurst.wiremock.client.WireMock.equalTo("cid-rate-test")));
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

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/exchange-rate?from=USD&to=EUR")));
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

        wireMock.verify(2, getRequestedFor(urlEqualTo("/api/exchange-rate?from=USD&to=EUR")));
    }

    @Test
    void noReintentaTasaNoEncontradaPorqueEsErrorFuncional() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=JPY&to=EUR"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "EXCHANGE_RATE_NOT_FOUND",
                                  "message": "Tasa no soportada"
                                }
                                """)));

        StepVerifier.create(exchangeRateService.obtenerTasa("JPY", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/exchange-rate?from=JPY&to=EUR")));
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

        wireMock.verify(2, getRequestedFor(urlEqualTo("/api/exchange-rate?from=USD&to=EUR")));
    }
}
