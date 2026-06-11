package com.novabank.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token emitido tras un login correcto.")
public record LoginResponseDTO(
        @Schema(description = "JWT Bearer. El ejemplo se omite para no exponer secretos.")
        String token,
        @Schema(example = "Bearer")
        String tipo,
        @Schema(description = "Expiracion del token en milisegundos epoch")
        long expiracion
) {
}
