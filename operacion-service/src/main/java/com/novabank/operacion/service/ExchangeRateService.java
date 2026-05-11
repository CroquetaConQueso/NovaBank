package com.novabank.operacion.service;

import com.novabank.operacion.dto.ExchangeRateResponseDTO;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@Service
public class ExchangeRateService {

    private final WebClient webClient;
    private final Duration timeout;

    public ExchangeRateService(
            WebClient.Builder webClientBuilder,
            @Value("${novabank.clients.exchange-rate-service.base-url:http://EXCHANGE-RATE-SERVICE}") String baseUrl,
            @Value("${novabank.clients.exchange-rate-service.timeout:3s}") Duration timeout
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.timeout = timeout;
    }

    public Mono<BigDecimal> obtenerTasa(String from, String to) {
        String fromNormalizado = normalizarDivisa(from, "monedaOrigen");
        String toNormalizado = normalizarDivisa(to, "monedaDestino");

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/exchange-rate")
                        .queryParam("from", fromNormalizado)
                        .queryParam("to", toNormalizado)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapearError)
                .bodyToMono(ExchangeRateResponseDTO.class)
                .timeout(timeout)
                .map(ExchangeRateResponseDTO::tasa)
                .flatMap(this::validarTasa)
                .onErrorMap(WebClientRequestException.class, this::servicioNoDisponible)
                .onErrorMap(TimeoutException.class, this::timeout)
                .onErrorMap(ex -> !(ex instanceof ExchangeRateUnavailableException) && !(ex instanceof ValidationException),
                        ex -> new ExchangeRateUnavailableException("No se pudo obtener una tasa de cambio fiable", ex));
    }

    private String normalizarDivisa(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException("La " + campo + " es obligatoria");
        }

        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private Mono<BigDecimal> validarTasa(BigDecimal tasa) {
        if (tasa == null || tasa.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new ExchangeRateUnavailableException("La tasa de cambio recibida no es valida"));
        }

        return Mono.just(tasa);
    }

    private Mono<? extends Throwable> mapearError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new ExchangeRateUnavailableException(mensajeRemoto(
                        body,
                        "No hay una tasa de cambio disponible para la transferencia"
                )));
    }

    private String mensajeRemoto(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }

        return fallback + ": " + body;
    }

    private ExchangeRateUnavailableException servicioNoDisponible(WebClientRequestException ex) {
        return new ExchangeRateUnavailableException("exchange-rate-service no esta disponible", ex);
    }

    private ExchangeRateUnavailableException timeout(TimeoutException ex) {
        return new ExchangeRateUnavailableException("exchange-rate-service no respondio a tiempo", ex);
    }
}
