package com.novabank.cuenta.application.port.out;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AlertaSaldoBajoPublisherPort {

    Mono<Void> publicar(MovimientoEventDTO movimiento, BigDecimal umbral);
}
