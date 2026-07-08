package com.novabank.operacion.application.port.in;

import reactor.core.publisher.Mono;

public interface SolicitarDepositoUseCase {

    Mono<OperacionAceptadaResult> solicitarDeposito(SolicitarDepositoCommand command);
}
