package com.novabank.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGatewayRoutesConfig {

    @Bean
    public RouteLocator openApiRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("openapi-auth-server", route -> route
                        .path("/v3/api-docs/auth-server")
                        .filters(filter -> filter.rewritePath("/v3/api-docs/auth-server", "/v3/api-docs"))
                        .uri("http://localhost:9000"))
                .route("openapi-cliente-service", route -> route
                        .path("/v3/api-docs/cliente-service")
                        .filters(filter -> filter.rewritePath("/v3/api-docs/cliente-service", "/v3/api-docs"))
                        .uri("http://localhost:8081"))
                .route("openapi-cuenta-service", route -> route
                        .path("/v3/api-docs/cuenta-service")
                        .filters(filter -> filter.rewritePath("/v3/api-docs/cuenta-service", "/v3/api-docs"))
                        .uri("http://localhost:8082"))
                .route("openapi-operacion-service", route -> route
                        .path("/v3/api-docs/operacion-service")
                        .filters(filter -> filter.rewritePath("/v3/api-docs/operacion-service", "/v3/api-docs"))
                        .uri("http://localhost:8083"))
                .route("openapi-exchange-rate-mock-service", route -> route
                        .path("/v3/api-docs/exchange-rate-mock-service")
                        .filters(filter -> filter.rewritePath("/v3/api-docs/exchange-rate-mock-service", "/v3/api-docs"))
                        .uri("http://localhost:8084"))
                .build();
    }
}
