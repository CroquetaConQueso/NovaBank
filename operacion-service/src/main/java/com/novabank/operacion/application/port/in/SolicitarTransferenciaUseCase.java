package com.novabank.operacion.application.port.in;

import reactor.core.publisher.Mono;

public interface SolicitarTransferenciaUseCase {

    Mono<TransferenciaAceptadaResult> solicitarTransferencia(SolicitarTransferenciaCommand command);
}
