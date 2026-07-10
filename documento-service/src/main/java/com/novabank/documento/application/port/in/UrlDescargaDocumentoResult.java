package com.novabank.documento.application.port.in;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record UrlDescargaDocumentoResult(
        UUID operacionId,
        URI url,
        Instant expiraEn
) {
}
