package com.novabank.operacion.application.port.in;

import reactor.core.publisher.Mono;

public interface ConsultarEstadoOperacionUseCase {

    Mono<EstadoOperacionAsincronaResult> consultar(ConsultarEstadoOperacionQuery query);
}
