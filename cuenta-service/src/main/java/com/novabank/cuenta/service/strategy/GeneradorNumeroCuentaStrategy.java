package com.novabank.cuenta.service.strategy;

import reactor.core.publisher.Mono;

public interface GeneradorNumeroCuentaStrategy {

    Mono<String> generarNumeroCuenta();
}
