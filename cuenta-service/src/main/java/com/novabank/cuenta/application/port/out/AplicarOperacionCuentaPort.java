package com.novabank.cuenta.application.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AplicarOperacionCuentaPort {

    Mono<Void> depositar(Long cuentaId, BigDecimal importe);

    Mono<Void> retirar(Long cuentaId, BigDecimal importe);

    Mono<Void> transferir(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal importe);
}
