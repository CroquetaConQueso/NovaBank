package com.novabank.documento.application.port.in;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EliminarDocumentoOperacionUseCase {

    Mono<Void> eliminarPorOperacion(UUID operacionId);
}
