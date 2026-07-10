package com.novabank.documento.application.port.out;

import com.novabank.documento.domain.model.DocumentoOperacion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

public interface DocumentoStoragePort {

    Mono<URI> generarUrlTemporalDescarga(UUID operacionId);

    Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId);

    Mono<Void> eliminarPorOperacion(UUID operacionId);

    Mono<Boolean> existePorOperacion(UUID operacionId);
}
