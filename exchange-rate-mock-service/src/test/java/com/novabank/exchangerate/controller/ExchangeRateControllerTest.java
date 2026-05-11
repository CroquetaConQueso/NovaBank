package com.novabank.exchangerate.controller;

import com.novabank.exchangerate.dto.ExchangeRateResponseDTO;
import com.novabank.exchangerate.exception.ExchangeRateNotFoundException;
import com.novabank.exchangerate.exception.GlobalExceptionHandler;
import com.novabank.exchangerate.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.when;

@WebFluxTest(ExchangeRateController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ExchangeRateControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    void obtenerTasaDevuelve200ConRespuesta() {
        when(exchangeRateService.obtenerTasa("USD", "EUR"))
                .thenReturn(Mono.just(new ExchangeRateResponseDTO(
                        "USD",
                        "EUR",
                        new BigDecimal("0.92"),
                        Instant.parse("2026-05-11T10:00:00Z")
                )));

        webTestClient.get()
                .uri("/api/exchange-rate?from=usd&to=eur")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.from").isEqualTo("USD")
                .jsonPath("$.to").isEqualTo("EUR")
                .jsonPath("$.tasa").isEqualTo(0.92)
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void obtenerTasaSinParametroDevuelve400() {
        webTestClient.get()
                .uri("/api/exchange-rate?from=USD")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST")
                .jsonPath("$.service").isEqualTo("exchange-rate-mock-service");
    }

    @Test
    void obtenerTasaConParametroBlankDevuelve400() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/exchange-rate")
                        .queryParam("from", " ")
                        .queryParam("to", "EUR")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void obtenerTasaNoSoportadaDevuelve404() {
        when(exchangeRateService.obtenerTasa("EUR", "GBP"))
                .thenReturn(Mono.error(new ExchangeRateNotFoundException("No existe tasa de cambio para el par EUR -> GBP")));

        webTestClient.get()
                .uri("/api/exchange-rate?from=EUR&to=GBP")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("EXCHANGE_RATE_NOT_FOUND")
                .jsonPath("$.service").isEqualTo("exchange-rate-mock-service");
    }
}
