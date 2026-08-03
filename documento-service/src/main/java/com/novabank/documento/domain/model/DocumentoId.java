package com.novabank.documento.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DocumentoId(UUID value) {

    public DocumentoId {
        Objects.requireNonNull(value, "value no puede ser null");
    }

    public static DocumentoId nuevo() {
        return new DocumentoId(UUID.randomUUID());
    }
}
