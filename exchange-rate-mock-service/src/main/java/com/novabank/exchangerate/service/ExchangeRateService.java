package com.novabank.exchangerate.service;

import com.novabank.exchangerate.dto.ExchangeRateResponseDTO;
import com.novabank.exchangerate.exception.ExchangeRateNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
public class ExchangeRateService {

    private final Map<String, BigDecimal> tasas = Map.of(
            "USD_EUR", new BigDecimal("0.92"),
            "GBP_EUR", new BigDecimal("1.17"),
            "EUR_USD", new BigDecimal("1.09")
    );

    public Mono<ExchangeRateResponseDTO> obtenerTasa(String from, String to) {
        return Mono.defer(() -> {
            String clave = from + "_" + to;
            BigDecimal tasa = tasas.get(clave);

            if (tasa == null) {
                return Mono.error(new ExchangeRateNotFoundException(
                        "No existe tasa de cambio para el par " + from + " -> " + to
                ));
            }

            return Mono.just(new ExchangeRateResponseDTO(from, to, tasa, Instant.now()));
        });
    }
}
