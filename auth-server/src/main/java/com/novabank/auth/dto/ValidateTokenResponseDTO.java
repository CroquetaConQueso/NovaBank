package com.novabank.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de validacion de JWT usado por api-gateway.")
public record ValidateTokenResponseDTO(
        @Schema(example = "true")
        boolean valido,
        @Schema(example = "usuario.demo")
        String username
) {
}
