package com.novabank.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.gateway.client.AuthValidationClient;
import com.novabank.gateway.dto.ValidateTokenResponseDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationGatewayFilterTest {

    private final AuthValidationClient authValidationClient = mock(AuthValidationClient.class);
    private final JwtAuthenticationGatewayFilter filter = new JwtAuthenticationGatewayFilter(
            authValidationClient,
            new GatewayErrorWriter(new ObjectMapper())
    );

    @Test
    void rutaPublicaNoExigeToken() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/auth/login", null);

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isTrue();
        verify(authValidationClient, never()).validate(org.mockito.Mockito.anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate",
            "/actuator/health",
            "/actuator/info"
    })
    void rutasPublicasNoExigenToken(String path) {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(path, null);

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isTrue();
        verify(authValidationClient, never()).validate(org.mockito.Mockito.anyString());
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401Json() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/clientes", null);

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("UNAUTHORIZED", "Authorization Bearer token obligatorio", "api-gateway", "timestamp");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/clientes",
            "/api/cuentas",
            "/api/operaciones"
    })
    void rutasProtegidasSinTokenDevuelven401(String path) {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(path, null);

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authorizationSinBearerDevuelve401() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/clientes", "Basic token");

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authValidationClient, never()).validate(org.mockito.Mockito.anyString());
    }

    @Test
    void bearerVacioDevuelve401() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/clientes", "Bearer ");

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authValidationClient, never()).validate(org.mockito.Mockito.anyString());
    }

    @Test
    void rutaProtegidaConTokenInvalidoDevuelve401Json() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/clientes", "Bearer invalid-token");
        when(authValidationClient.validate("invalid-token"))
                .thenReturn(Mono.just(new ValidateTokenResponseDTO(false, null)));

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("INVALID_TOKEN", "api-gateway");
    }

    @Test
    void rutaProtegidaConTokenValidoContinuaLaCadena() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/clientes", "Bearer valid-token");
        when(authValidationClient.validate("valid-token"))
                .thenReturn(Mono.just(new ValidateTokenResponseDTO(true, "ana")));

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void errorDeAuthServerDevuelve503Json() {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/cuentas/1", "Bearer any-token");
        when(authValidationClient.validate("any-token"))
                .thenReturn(Mono.error(new IllegalStateException("auth down")));

        StepVerifier.create(filter.filter(exchange, chain(chainCalled))).verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("AUTH_SERVICE_UNAVAILABLE", "api-gateway");
    }

    private MockServerWebExchange exchange(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (authorization != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MockServerWebExchange.from(builder);
    }

    private GatewayFilterChain chain(AtomicBoolean called) {
        return exchange -> {
            called.set(true);
            return Mono.empty();
        };
    }
}
