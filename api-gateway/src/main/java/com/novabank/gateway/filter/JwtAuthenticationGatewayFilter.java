package com.novabank.gateway.filter;

import com.novabank.gateway.client.AuthValidationClient;
import com.novabank.gateway.dto.ValidateTokenResponseDTO;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthValidationClient authValidationClient;
    private final GatewayErrorWriter errorWriter;

    public JwtAuthenticationGatewayFilter(
            AuthValidationClient authValidationClient,
            GatewayErrorWriter errorWriter
    ) {
        this.authValidationClient = authValidationClient;
        this.errorWriter = errorWriter;
    }

    /**
     * El Gateway delega la validez del JWT en auth-server; no interpreta la
     * firma ni las claims para mantener una unica autoridad de autenticacion.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!isProtected(path) || isPublic(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX) || authorization.length() <= BEARER_PREFIX.length()) {
            return errorWriter.write(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Authorization Bearer token obligatorio"
            );
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            return errorWriter.write(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Authorization Bearer token obligatorio"
            );
        }

        return authValidationClient.validate(token)
                .flatMap(response -> handleValidationResponse(exchange, chain, response))
                .onErrorResume(ex -> errorWriter.write(
                        exchange,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AUTH_SERVICE_UNAVAILABLE",
                        "auth-server no esta disponible para validar el token"
                ));
    }

    private Mono<Void> handleValidationResponse(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ValidateTokenResponseDTO response
    ) {
        if (response != null && response.valido()) {
            return chain.filter(exchange);
        }

        return errorWriter.write(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "INVALID_TOKEN",
                "Token invalido o expirado"
        );
    }

    /**
     * Los endpoints de auth quedan disponibles para emitir y validar tokens
     * antes de aplicar el filtro a las rutas de negocio.
     */
    private boolean isPublic(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/validate")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }

    /**
     * Mantiene el control de acceso perimetral en el Gateway sin modificar los
     * contratos de los microservicios internos.
     */
    private boolean isProtected(String path) {
        return path.startsWith("/api/clientes/")
                || path.equals("/api/clientes")
                || path.startsWith("/api/cuentas/")
                || path.equals("/api/cuentas")
                || path.startsWith("/api/operaciones/")
                || path.equals("/api/operaciones");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
