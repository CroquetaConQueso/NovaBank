package com.novabank.exchangerate.service;

import com.novabank.exchangerate.exception.ExchangeRateNotFoundException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceTest {

    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    @Test
    void obtenerTasaSoportadaDevuelveRespuesta() {
        StepVerifier.create(exchangeRateService.obtenerTasa("USD", "EUR"))
                .assertNext(response -> {
                    assertThat(response.from()).isEqualTo("USD");
                    assertThat(response.to()).isEqualTo("EUR");
                    assertThat(response.tasa()).isEqualByComparingTo(new BigDecimal("0.92"));
                    assertThat(response.timestamp()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void obtenerTasaNoSoportadaDevuelveErrorFuncional() {
        StepVerifier.create(exchangeRateService.obtenerTasa("EUR", "GBP"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ExchangeRateNotFoundException.class);
                    assertThat(error).hasMessageContaining("EUR -> GBP");
                })
                .verify();
    }
}
