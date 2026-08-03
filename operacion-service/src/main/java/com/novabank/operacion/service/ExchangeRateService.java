package com.novabank.operacion.service;

import com.novabank.operacion.dto.ExchangeRateResponseDTO;
import com.novabank.operacion.dto.ExchangeRateResultDTO;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.tracing.CorrelationIdSupport;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final WebClient webClient;
    private final Duration timeout;
    private final CircuitBreaker exchangeRateCircuitBreaker;
    private final Retry exchangeRateRetry;
    private final ExchangeRateCache exchangeRateCache;

    @Autowired
    public ExchangeRateService(
            WebClient.Builder webClientBuilder,
            @Value("${novabank.clients.exchange-rate-service.base-url:http://exchange-rate-mock-service}") String baseUrl,
            @Value("${novabank.clients.exchange-rate-service.timeout:3s}") Duration timeout,
            ExchangeRateCache exchangeRateCache
    ) {
        this(
                webClientBuilder,
                baseUrl,
                timeout,
                CircuitBreaker.of("exchangeRateCircuitBreaker", circuitBreakerConfig()),
                Retry.of("exchangeRateRetry", retryConfig()),
                exchangeRateCache
        );
    }

    ExchangeRateService(
            WebClient.Builder webClientBuilder,
            String baseUrl,
            Duration timeout,
            CircuitBreaker exchangeRateCircuitBreaker,
            Retry exchangeRateRetry,
            ExchangeRateCache exchangeRateCache
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.timeout = timeout;
        this.exchangeRateCircuitBreaker = exchangeRateCircuitBreaker;
        this.exchangeRateRetry = exchangeRateRetry;
        this.exchangeRateCache = exchangeRateCache;
    }

    public Mono<BigDecimal> obtenerTasa(String from, String to) {
        return obtenerTasaConOrigen(from, to)
                .map(ExchangeRateResultDTO::tasa);
    }

    public Mono<ExchangeRateResultDTO> obtenerTasaConOrigen(String from, String to) {
        String fromNormalizado = normalizarDivisa(from, "monedaOrigen");
        String toNormalizado = normalizarDivisa(to, "monedaDestino");

        Mono<ExchangeRateResultDTO> llamadaRemota = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/exchange-rate")
                        .queryParam("from", fromNormalizado)
                        .queryParam("to", toNormalizado)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapearError)
                .bodyToMono(ExchangeRateResponseDTO.class)
                .timeout(timeout)
                .doOnEach(signal -> {
                    if (signal.isOnNext()) {
                        log.info(
                                "correlationId={} tasa recibida from={} to={}",
                                CorrelationIdSupport.fromContext(signal.getContextView()),
                                fromNormalizado,
                                toNormalizado
                        );
                    }
                    if (signal.isOnError()) {
                        log.warn(
                                "correlationId={} fallo consultando tasa from={} to={}",
                                CorrelationIdSupport.fromContext(signal.getContextView()),
                                fromNormalizado,
                                toNormalizado
                        );
                    }
                })
                .map(ExchangeRateResponseDTO::tasa)
                .flatMap(this::validarTasa)
                .doOnNext(tasa -> exchangeRateCache.guardar(fromNormalizado, toNormalizado, tasa))
                .map(tasa -> new ExchangeRateResultDTO(tasa, false, java.time.Instant.now()))
                .onErrorMap(WebClientRequestException.class, this::servicioNoDisponible)
                .onErrorMap(TimeoutException.class, this::timeout)
                .onErrorMap(ex -> !(ex instanceof ExchangeRateUnavailableException) && !(ex instanceof ValidationException),
                        ex -> new ExchangeRateUnavailableException("No se pudo obtener una tasa de cambio fiable", ex));

        return llamadaRemota
                .transformDeferred(RetryOperator.of(exchangeRateRetry))
                .transformDeferred(CircuitBreakerOperator.of(exchangeRateCircuitBreaker))
                .onErrorMap(
                        CallNotPermittedException.class,
                        error -> new ExchangeRateUnavailableException("exchange-rate-service no esta disponible", error)
                )
                .onErrorResume(error -> resolverConCacheSiEsTecnico(error, fromNormalizado, toNormalizado));
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
                .map(body -> {
                    String mensaje = mensajeRemoto(
                            body,
                            "No hay una tasa de cambio disponible para la transferencia"
                    );
                    if (response.statusCode().is5xxServerError()) {
                        return new RetryableExchangeRateUnavailableException(mensaje);
                    }
                    return new ExchangeRateUnavailableException(mensaje);
                });
    }

    private String mensajeRemoto(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }

        return fallback + ": " + body;
    }

    private ExchangeRateUnavailableException servicioNoDisponible(WebClientRequestException ex) {
        return new RetryableExchangeRateUnavailableException("exchange-rate-service no esta disponible", ex);
    }

    private ExchangeRateUnavailableException timeout(TimeoutException ex) {
        return new RetryableExchangeRateUnavailableException("exchange-rate-service no respondio a tiempo", ex);
    }

    private Mono<ExchangeRateResultDTO> resolverConCacheSiEsTecnico(
            Throwable error,
            String from,
            String to
    ) {
        // Reutiliza una tasa reciente solo ante fallos tecnicos del proveedor.
        if (!esFalloTecnico(error)) {
            return Mono.error(error);
        }

        return exchangeRateCache.obtener(from, to)
                .map(cached -> {
                    log.warn("usando tasa cacheada from={} to={}", from, to);
                    return Mono.just(cached);
                })
                .orElseGet(() -> Mono.error(error));
    }

    private boolean esFalloTecnico(Throwable error) {
        return error instanceof RetryableExchangeRateUnavailableException
                || error.getCause() instanceof RetryableExchangeRateUnavailableException
                || error.getCause() instanceof CallNotPermittedException;
    }

    private static CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .permittedNumberOfCallsInHalfOpenState(1)
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .recordExceptions(RetryableExchangeRateUnavailableException.class)
                .ignoreExceptions(ValidationException.class)
                .build();
    }

    private static RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RetryableExchangeRateUnavailableException.class)
                .ignoreExceptions(ValidationException.class)
                .build();
    }

    private static class RetryableExchangeRateUnavailableException extends ExchangeRateUnavailableException {

        RetryableExchangeRateUnavailableException(String message) {
            super(message);
        }

        RetryableExchangeRateUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
