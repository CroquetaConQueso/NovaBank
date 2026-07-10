package com.novabank.documento.application.port.in;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GenerarUrlDescargaDocumentoUseCase {

    Mono<UrlDescargaDocumentoResult> generarUrlDescarga(UUID operacionId);
}
