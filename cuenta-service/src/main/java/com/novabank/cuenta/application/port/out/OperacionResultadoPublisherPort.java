package com.novabank.cuenta.application.port.out;

import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaCommand;
import reactor.core.publisher.Mono;

public interface OperacionResultadoPublisherPort {

    Mono<Void> publicarCompletada(ProcesarOperacionSolicitadaCommand solicitud);

    Mono<Void> publicarFallida(ProcesarOperacionSolicitadaCommand solicitud, String codigoError, String motivo);
}
