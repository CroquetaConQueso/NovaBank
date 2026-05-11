package com.novabank.cuenta.client;

import com.novabank.cuenta.dto.ClienteResponseDTO;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ClienteServiceClient {

    private final WebClient webClient;

    public ClienteServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${novabank.clients.cliente-service.base-url:http://CLIENTE-SERVICE}") String baseUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<ClienteResponseDTO> obtenerCliente(Long id) {
        return webClient.get()
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
    }

    private boolean esFalloTecnico(Throwable error) {
        return !(error instanceof ResourceNotFoundException)
                && !(error instanceof RemoteServiceException);
    }
}
