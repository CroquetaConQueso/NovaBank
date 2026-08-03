package com.novabank.documento.adapter.in.web;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DocumentoUrlResponseDTO(
        UUID operacionId,
        URI url,
        Instant expiraEn
) {
}
