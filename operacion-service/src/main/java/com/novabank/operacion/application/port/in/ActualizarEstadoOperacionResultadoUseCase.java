package com.novabank.operacion.application.port.in;

import reactor.core.publisher.Mono;

public interface ActualizarEstadoOperacionResultadoUseCase {

    Mono<ActualizarOperacionResultadoResult> actualizar(ActualizarOperacionResultadoCommand command);
}
