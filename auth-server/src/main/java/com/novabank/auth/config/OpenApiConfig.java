package com.novabank.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NovaBank Auth API",
                version = "4.0-SNAPSHOT",
                description = "API HTTP para registro, login y validacion de tokens JWT."
        ),
        servers = {
                @Server(url = "/", description = "Servicio local o ruta publicada por Gateway")
        }
)
public class OpenApiConfig {
}
