package com.novabank.operacion.application.port.in;

import reactor.core.publisher.Mono;

public interface SolicitarRetiradaUseCase {

    Mono<OperacionAceptadaResult> solicitarRetirada(SolicitarRetiradaCommand command);
}
