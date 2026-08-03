package com.novabank.operacion.application.port.out;

import com.novabank.operacion.application.usecase.ComisionCalculada;
import com.novabank.operacion.application.usecase.ComisionCommand;
import reactor.core.publisher.Mono;

public interface ComisionCalculatorPort {

    Mono<ComisionCalculada> calcularComision(ComisionCommand command);
}
