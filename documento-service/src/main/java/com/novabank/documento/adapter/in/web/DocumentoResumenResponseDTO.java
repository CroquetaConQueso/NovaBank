package com.novabank.documento.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

public record DocumentoResumenResponseDTO(
        UUID documentoId,
        UUID operacionId,
        Long cuentaId,
        String tipoDocumento,
        String contentType,
        Instant creadoEn
) {
}
