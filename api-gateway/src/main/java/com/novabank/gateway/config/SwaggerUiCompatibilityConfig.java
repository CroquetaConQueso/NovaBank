package com.novabank.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class SwaggerUiCompatibilityConfig {

    @Bean
    public RouterFunction<ServerResponse> swaggerUiIndexRedirectRoute() {
        return RouterFunctions.route(
                RequestPredicates.GET("/swagger-ui/index.html"),
                request -> ServerResponse.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, "/swagger-ui.html")
                        .build()
        );
    }
}
