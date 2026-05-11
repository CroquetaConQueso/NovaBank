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
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class CuentaServiceClient {

    private static final String SERVICE_NAME = "cuenta-service";

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
                .onStatus(HttpStatusCode::isError, this::mapearError)
                .bodyToMono(CuentaResponseDTO.class)
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException(SERVICE_NAME + " no devolvio datos de la cuenta")))
                .onErrorMap(WebClientRequestException.class, this::servicioNoDisponible);
    }

    public Mono<CuentaResponseDTO> retirar(Long id, CuentaOperacionRequestDTO request) {
        return webClient.post()
                .uri("/internal/cuentas/{id}/retiros", id)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapearError)
                .bodyToMono(CuentaResponseDTO.class)
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException(SERVICE_NAME + " no devolvio datos de la cuenta")))
                .onErrorMap(WebClientRequestException.class, this::servicioNoDisponible);
    }

    public Mono<AplicarMovimientoResponseDTO> aplicarMovimiento(AplicarMovimientoRequestDTO request) {
        return webClient.post()
                .uri("/internal/cuentas/aplicar-movimientos")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapearError)
                .bodyToMono(AplicarMovimientoResponseDTO.class)
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException(SERVICE_NAME + " no devolvio datos de la transferencia")))
                .onErrorMap(WebClientRequestException.class, this::servicioNoDisponible);
    }

    private Mono<? extends Throwable> mapearError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> switch (response.statusCode().value()) {
                    case 404 -> new RemoteResourceNotFoundException(mensajeRemoto(body, "Recurso no encontrado en cuenta-service"));
                    case 400, 422 -> new RemoteValidationException(mensajeRemoto(body, "Solicitud rechazada por cuenta-service"));
                    case 409 -> new RemoteConflictException(mensajeRemoto(body, "Conflicto al aplicar operacion en cuenta-service"));
                    default -> {
                        if (response.statusCode().is5xxServerError()) {
                            yield new RemoteServiceException(mensajeRemoto(body, "cuenta-service no esta disponible"));
                        }
                        yield new RemoteServiceException(mensajeRemoto(body, "Error remoto de cuenta-service"));
                    }
                });
    }

    private String mensajeRemoto(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }

        return fallback + ": " + body;
    }

    private RemoteServiceException servicioNoDisponible(WebClientRequestException ex) {
        return new RemoteServiceException("cuenta-service no esta disponible", ex);
    }
}
