package com.novabank.documento.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DocumentoOperacion(
        DocumentoId documentoId,
        UUID operacionId,
        Long cuentaId,
        String claveObjeto,
        TipoDocumento tipoDocumento,
        String contentType,
        Instant creadoEn
) {

    public DocumentoOperacion {
        Objects.requireNonNull(documentoId, "documentoId no puede ser null");
        Objects.requireNonNull(operacionId, "operacionId no puede ser null");
        Objects.requireNonNull(cuentaId, "cuentaId no puede ser null");
        Objects.requireNonNull(claveObjeto, "claveObjeto no puede ser null");
        Objects.requireNonNull(tipoDocumento, "tipoDocumento no puede ser null");
        Objects.requireNonNull(contentType, "contentType no puede ser null");
        Objects.requireNonNull(creadoEn, "creadoEn no puede ser null");
    }
}
