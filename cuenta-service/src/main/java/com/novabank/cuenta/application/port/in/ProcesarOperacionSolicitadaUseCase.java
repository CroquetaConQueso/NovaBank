package com.novabank.cuenta.application.port.in;

import reactor.core.publisher.Mono;

public interface ProcesarOperacionSolicitadaUseCase {

    Mono<ProcesarOperacionSolicitadaResultado> procesar(ProcesarOperacionSolicitadaCommand command);
}
