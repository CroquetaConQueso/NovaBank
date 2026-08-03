package com.novabank.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NovaBank API Gateway",
                version = "4.0-SNAPSHOT",
                description = "Entrada HTTP al ecosistema NovaBank y vista agregada de documentacion OpenAPI."
        ),
        servers = {
                @Server(url = "/", description = "API Gateway local")
        }
)
public class OpenApiConfig {
}
