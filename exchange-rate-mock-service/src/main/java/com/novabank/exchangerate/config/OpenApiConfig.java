package com.novabank.exchangerate.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NovaBank Exchange Rate Mock API",
                version = "4.0-SNAPSHOT",
                description = "API HTTP mock para tasas de cambio usadas por transferencias en divisa."
        ),
        servers = {
                @Server(url = "/", description = "Servicio local o ruta publicada por Gateway")
        }
)
public class OpenApiConfig {
}
