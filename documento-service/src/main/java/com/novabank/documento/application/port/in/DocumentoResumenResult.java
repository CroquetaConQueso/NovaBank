package com.novabank.documento.application.port.in;

import com.novabank.documento.domain.model.TipoDocumento;

import java.time.Instant;
import java.util.UUID;

public record DocumentoResumenResult(
        UUID documentoId,
        UUID operacionId,
        Long cuentaId,
        TipoDocumento tipoDocumento,
        String contentType,
        Instant creadoEn
) {
}
