package com.novabank.operacion.client;

import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.AplicarMovimientoResponseDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.exception.RemoteConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class CuentaServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker cuentaServiceCircuitBreaker;
    private final Retry cuentaServiceRetry;

    @Autowired
    public CuentaServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${novabank.clients.cuenta-service.base-url:http://CUENTA-SERVICE}") String baseUrl
    ) {
        this(
                webClientBuilder,
                baseUrl,
                CircuitBreaker.of("cuentaServiceCircuitBreaker", circuitBreakerConfig()),
                Retry.of("cuentaServiceRetry", retryConfig())
        );
    }

    CuentaServiceClient(
            WebClient.Builder webClientBuilder,
            String baseUrl,
            CircuitBreaker cuentaServiceCircuitBreaker,
            Retry cuentaServiceRetry
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.cuentaServiceCircuitBreaker = cuentaServiceCircuitBreaker;
        this.cuentaServiceRetry = cuentaServiceRetry;
    }

    public Mono<CuentaResponseDTO> depositar(Long id, CuentaOperacionRequestDTO request) {
        Mono<CuentaResponseDTO> llamadaRemota = webClient.post()
                .uri("/internal/cuentas/{id}/depositos", id)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new RemoteResourceNotFoundException("La cuenta indicada no existe")))
                .onStatus(status -> status == HttpStatus.UNPROCESSABLE_ENTITY || status == HttpStatus.BAD_REQUEST,
                        response -> Mono.error(new RemoteValidationException("cuenta-service rechazo la peticion interna")))
                .onStatus(status -> status == HttpStatus.CONFLICT,
                        response -> Mono.error(new RemoteConflictException("La operacion no pudo completarse por conflicto")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new RemoteServiceException("cuenta-service no esta disponible")))
                .bodyToMono(CuentaResponseDTO.class)
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .onErrorMap(this::esFalloTecnico,
                        error -> new RemoteServiceException("cuenta-service no esta disponible", error));

        return protegerSinRetry(llamadaRemota);
    }

    public Mono<CuentaResponseDTO> retirar(Long id, CuentaOperacionRequestDTO request) {
        Mono<CuentaResponseDTO> llamadaRemota = webClient.post()
                .uri("/internal/cuentas/{id}/retiros", id)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new RemoteResourceNotFoundException("La cuenta indicada no existe")))
                .onStatus(status -> status == HttpStatus.UNPROCESSABLE_ENTITY || status == HttpStatus.BAD_REQUEST,
                        response -> Mono.error(new RemoteValidationException("La operacion fue rechazada por cuenta-service")))
                .onStatus(status -> status == HttpStatus.CONFLICT,
                        response -> Mono.error(new RemoteConflictException("La operacion no pudo completarse por conflicto")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new RemoteServiceException("cuenta-service no esta disponible")))
                .bodyToMono(CuentaResponseDTO.class)
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .onErrorMap(this::esFalloTecnico,
                        error -> new RemoteServiceException("cuenta-service no esta disponible", error));

        return protegerSinRetry(llamadaRemota);
    }

    public Mono<AplicarMovimientoResponseDTO> aplicarMovimiento(AplicarMovimientoRequestDTO request) {
        Mono<AplicarMovimientoResponseDTO> llamadaRemota = webClient.post()
                .uri("/internal/cuentas/aplicar-movimientos")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new RemoteResourceNotFoundException("La cuenta indicada no existe")))
                .onStatus(status -> status == HttpStatus.UNPROCESSABLE_ENTITY || status == HttpStatus.BAD_REQUEST,
                        response -> Mono.error(new RemoteValidationException("La operacion fue rechazada por cuenta-service")))
                .onStatus(status -> status == HttpStatus.CONFLICT,
                        response -> Mono.error(new RemoteConflictException("La operacion no pudo completarse por conflicto")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new RemoteServiceException("cuenta-service no esta disponible")))
                .bodyToMono(AplicarMovimientoResponseDTO.class)
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la transferencia")))
                .onErrorMap(this::esFalloTecnico,
                        error -> new RemoteServiceException("cuenta-service no esta disponible", error));

        return llamadaRemota
                .transformDeferred(RetryOperator.of(cuentaServiceRetry))
                .transformDeferred(CircuitBreakerOperator.of(cuentaServiceCircuitBreaker))
                .onErrorMap(CallNotPermittedException.class, this::circuitoAbierto);
    }

    private boolean esFalloTecnico(Throwable error) {
        return !(error instanceof RemoteResourceNotFoundException)
                && !(error instanceof RemoteValidationException)
                && !(error instanceof RemoteConflictException)
                && !(error instanceof RemoteServiceException);
    }

    private <T> Mono<T> protegerSinRetry(Mono<T> llamadaRemota) {
        return llamadaRemota
                .transformDeferred(CircuitBreakerOperator.of(cuentaServiceCircuitBreaker))
                .onErrorMap(CallNotPermittedException.class, this::circuitoAbierto);
    }

    private RemoteServiceException circuitoAbierto(CallNotPermittedException error) {
        return new RemoteServiceException("cuenta-service no esta disponible", error);
    }

    private static CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .permittedNumberOfCallsInHalfOpenState(1)
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .recordExceptions(RemoteServiceException.class)
                .ignoreExceptions(
                        RemoteResourceNotFoundException.class,
                        RemoteValidationException.class,
                        RemoteConflictException.class
                )
                .build();
    }

    private static RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RemoteServiceException.class)
                .ignoreExceptions(
                        RemoteResourceNotFoundException.class,
                        RemoteValidationException.class,
                        RemoteConflictException.class
                )
                .build();
    }
}
