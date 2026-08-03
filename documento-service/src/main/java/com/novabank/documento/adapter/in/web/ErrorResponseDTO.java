package com.novabank.documento.adapter.in.web;

import java.time.Instant;

public record ErrorResponseDTO(
        String codigo,
        String mensaje,
        Instant timestamp
) {
}
