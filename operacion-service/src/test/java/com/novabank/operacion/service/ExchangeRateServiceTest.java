package com.novabank.operacion.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.benmanes.caffeine.cache.Ticker;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.config.WebClientConfig;
import com.novabank.operacion.tracing.CorrelationIdSupport;
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
    private FakeTicker ticker;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        ticker = new FakeTicker();
        exchangeRateService = new ExchangeRateService(
                new WebClientConfig().webClientBuilder(),
                wireMock.getRuntimeInfo().getHttpBaseUrl(),
                Duration.ofMillis(500),
                new ExchangeRateCache(ticker)
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
                        .withFixedDelay(750)
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

    @Test
    void tasaRemotaValidaSeCacheaYFalloTecnicoPosteriorUsaCacheVigente() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "USD",
                          "to": "EUR",
                          "tasa": 0.92,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .assertNext(result -> {
                    assertThat(result.tasa()).isEqualByComparingTo("0.92");
                    assertThat(result.cacheada()).isFalse();
                })
                .verifyComplete();

        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .assertNext(result -> {
                    assertThat(result.tasa()).isEqualByComparingTo("0.92");
                    assertThat(result.cacheada()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void falloTecnicoSinCacheVigenteMantieneErrorSeguro() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void cacheVencidaNoSeUsa() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "USD",
                          "to": "EUR",
                          "tasa": 0.92,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerTasa("USD", "EUR"))
                .assertNext(tasa -> assertThat(tasa).isEqualByComparingTo("0.92"))
                .verifyComplete();

        ticker.advance(Duration.ofMinutes(6));
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void tasaInvalidaNoSeCachea() {
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(okJson("""
                        {
                          "from": "USD",
                          "to": "EUR",
                          "tasa": -1,
                          "timestamp": "2026-05-11T10:00:00Z"
                        }
                        """)));

        StepVerifier.create(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();

        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/exchange-rate?from=USD&to=EUR"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();
    }

    @Test
    void exchangeRateCacheNormalizaClaveYRespetaTtl() {
        ExchangeRateCache cache = new ExchangeRateCache(ticker);
        cache.guardar(" usd ", " eur ", new BigDecimal("0.92"));

        assertThat(cache.obtener("USD", "EUR")).isPresent();

        ticker.advance(Duration.ofMinutes(6));

        assertThat(cache.obtener("USD", "EUR")).isEmpty();
    }

    private static class FakeTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
