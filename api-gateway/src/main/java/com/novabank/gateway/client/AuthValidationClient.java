package com.novabank.gateway.client;

import com.novabank.gateway.dto.ValidateTokenResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthValidationClient {

    private final WebClient webClient;
    private final String validationUrl;

    public AuthValidationClient(
            WebClient.Builder webClientBuilder,
            @Value("${novabank.gateway.auth-validation-url:lb://AUTH-SERVER/api/auth/validate}") String validationUrl
    ) {
        this.webClient = webClientBuilder.build();
        this.validationUrl = validationUrl;
    }

    /**
     * Usa la URL lb:// configurada para resolver auth-server mediante Eureka
     * sin fijar host ni puerto en el filtro.
     */
    public Mono<ValidateTokenResponseDTO> validate(String token) {
        return webClient.get()
                .uri(validationUrl + "?token={token}", token)
                .retrieve()
                .bodyToMono(ValidateTokenResponseDTO.class);
    }
}
