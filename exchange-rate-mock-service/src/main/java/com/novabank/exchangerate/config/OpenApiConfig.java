package com.novabank.exchangerate.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NovaBank - Tipo de Cambio Mock",
                version = "4.0-SNAPSHOT",
                description = "Proveedor externo simulado de tasas de cambio."
        ),
        servers = {
                @Server(url = "/", description = "API Gateway")
        }
)
public class OpenApiConfig {
}
