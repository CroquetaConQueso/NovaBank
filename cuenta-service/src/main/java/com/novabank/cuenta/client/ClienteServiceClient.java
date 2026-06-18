package com.novabank.cuenta.client;

import com.novabank.cuenta.dto.ClienteResponseDTO;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class ClienteServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker clienteServiceCircuitBreaker;
    private final Retry clienteServiceRetry;

    @Autowired
    public ClienteServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${novabank.clients.cliente-service.base-url:http://cliente-service}") String baseUrl
    ) {
        this(
                webClientBuilder,
                baseUrl,
                CircuitBreaker.of("clienteServiceCircuitBreaker", circuitBreakerConfig()),
                Retry.of("clienteServiceRetry", retryConfig())
        );
    }

    ClienteServiceClient(
            WebClient.Builder webClientBuilder,
            String baseUrl,
            CircuitBreaker clienteServiceCircuitBreaker,
            Retry clienteServiceRetry
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.clienteServiceCircuitBreaker = clienteServiceCircuitBreaker;
        this.clienteServiceRetry = clienteServiceRetry;
    }

    public Mono<ClienteResponseDTO> obtenerCliente(Long id) {
        Mono<ClienteResponseDTO> llamadaRemota = webClient.get()
                .uri("/api/clientes/{id}", id)
                .retrieve()
                .onStatus(
                        status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ResourceNotFoundException("No existe ningun cliente con el id indicado"))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> Mono.error(new RemoteServiceException("cliente-service no esta disponible"))
                )
                .bodyToMono(ClienteResponseDTO.class)
                .onErrorMap(
                        this::esFalloTecnico,
                        error -> new RemoteServiceException("cliente-service no esta disponible", error)
                );

        return llamadaRemota
                .transformDeferred(RetryOperator.of(clienteServiceRetry))
                .transformDeferred(CircuitBreakerOperator.of(clienteServiceCircuitBreaker))
                .onErrorMap(
                        CallNotPermittedException.class,
                        error -> new RemoteServiceException("cliente-service no esta disponible", error)
                );
    }

    private boolean esFalloTecnico(Throwable error) {
        return !(error instanceof ResourceNotFoundException)
                && !(error instanceof RemoteServiceException);
    }

    private static CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .permittedNumberOfCallsInHalfOpenState(1)
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .recordExceptions(RemoteServiceException.class)
                .ignoreExceptions(ResourceNotFoundException.class)
                .build();
    }

    private static RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RemoteServiceException.class)
                .ignoreExceptions(ResourceNotFoundException.class)
                .build();
    }
}
