package com.novabank.documento.application.port.out;

import java.net.URI;
import java.time.Instant;

public record DocumentoUrlTemporal(
        URI url,
        Instant expiraEn
) {
}
