package com.novabank.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.auth.dto.LoginRequestDTO;
import com.novabank.auth.dto.LoginResponseDTO;
import com.novabank.auth.dto.RegisterRequestDTO;
import com.novabank.auth.dto.RegisterResponseDTO;
import com.novabank.auth.dto.ValidateTokenResponseDTO;
import com.novabank.auth.exception.DuplicateUserException;
import com.novabank.auth.exception.GlobalExceptionHandler;
import com.novabank.auth.exception.InvalidCredentialsException;
import com.novabank.auth.config.SecurityConfig;
import com.novabank.auth.service.AuthService;
import com.novabank.auth.tracing.CorrelationIdWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CorrelationIdWebFilter.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void registerDevuelveCreated() throws Exception {
        when(authService.registrar(any(RegisterRequestDTO.class)))
                .thenReturn(Mono.just(new RegisterResponseDTO(1L, "ana", "USER", true, LocalDateTime.now())));

        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(new RegisterRequestDTO("ana", "password123")))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.username").isEqualTo("ana");
    }

    @Test
    void registerDuplicadoDevuelve409() throws Exception {
        when(authService.registrar(any(RegisterRequestDTO.class)))
                .thenReturn(Mono.error(new DuplicateUserException("Ya existe un usuario con ese username")));

        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(new RegisterRequestDTO("ana", "password123")))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT")
                .jsonPath("$.service").isEqualTo("auth-server");
    }

    @Test
    void loginCorrectoDevuelveTokenBearer() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(Mono.just(new LoginResponseDTO("jwt-token", "Bearer", 86400000L)));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(new LoginRequestDTO("ana", "password123")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwt-token")
                .jsonPath("$.tipo").isEqualTo("Bearer");
    }

    @Test
    void loginIncorrectoDevuelve401() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(Mono.error(new InvalidCredentialsException("Credenciales invalidas")));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", "cid-auth-test")
                .bodyValue(objectMapper.writeValueAsString(new LoginRequestDTO("ana", "bad-password")))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-Correlation-Id", "cid-auth-test")
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.correlationId").isEqualTo("cid-auth-test");
    }

    @Test
    void validateDevuelveResultado() throws Exception {
        when(authService.validarToken(eq("jwt-token")))
                .thenReturn(Mono.just(new ValidateTokenResponseDTO(true, "ana")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/auth/validate").queryParam("token", "jwt-token").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.valido").isEqualTo(true)
                .jsonPath("$.username").isEqualTo("ana");
    }

    @Test
    void loginRequestInvalidoDevuelveFieldErrors() throws Exception {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(new LoginRequestDTO("", "")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.username").exists()
                .jsonPath("$.fieldErrors.password").exists();
    }

    @Test
    void registerRequestInvalidoDevuelveFieldErrors() throws Exception {
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(new RegisterRequestDTO("", "")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.username").exists()
                .jsonPath("$.fieldErrors.password").exists();
    }

    @Test
    void registerConJsonMalformadoDevuelve400() throws Exception {
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void loginConJsonMalformadoDevuelve400() throws Exception {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void loginUsuarioInexistenteDevuelve401() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(Mono.error(new InvalidCredentialsException("Credenciales invalidas")));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(new LoginRequestDTO("nadie", "password123")))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void validateSinTokenDevuelve400() throws Exception {
        webTestClient.get()
                .uri("/api/auth/validate")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void validateTokenMalformadoDevuelveValidoFalse() throws Exception {
        when(authService.validarToken(eq("token-malformado")))
                .thenReturn(Mono.just(new ValidateTokenResponseDTO(false, null)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/auth/validate").queryParam("token", "token-malformado").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.valido").isEqualTo(false);
    }
}
