package com.novabank.operacion.application.port.out;

import com.novabank.operacion.domain.model.OperacionSolicitada;
import reactor.core.publisher.Mono;

public interface OperacionSolicitadaPublisherPort {

    Mono<Void> publicar(OperacionSolicitada operacion);
}
