package com.novabank.documento.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NovaBank Documento Service API")
                        .version("v1")
                        .description("API base para justificantes de operaciones. S3 se integrara en una iteracion posterior."));
    }
}
