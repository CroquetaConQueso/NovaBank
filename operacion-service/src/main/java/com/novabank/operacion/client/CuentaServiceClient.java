package com.novabank.operacion.client;

import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.AplicarMovimientoResponseDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.exception.RemoteConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CuentaServiceClient {

    private final WebClient webClient;

    public CuentaServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${novabank.clients.cuenta-service.base-url:http://CUENTA-SERVICE}") String baseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<CuentaResponseDTO> depositar(Long id, CuentaOperacionRequestDTO request) {
        return webClient.post()
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
    }

    public Mono<CuentaResponseDTO> retirar(Long id, CuentaOperacionRequestDTO request) {
        return webClient.post()
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
    }

    public Mono<AplicarMovimientoResponseDTO> aplicarMovimiento(AplicarMovimientoRequestDTO request) {
        return webClient.post()
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
    }

    private boolean esFalloTecnico(Throwable error) {
        return !(error instanceof RemoteResourceNotFoundException)
                && !(error instanceof RemoteValidationException)
                && !(error instanceof RemoteConflictException)
                && !(error instanceof RemoteServiceException);
    }
}
