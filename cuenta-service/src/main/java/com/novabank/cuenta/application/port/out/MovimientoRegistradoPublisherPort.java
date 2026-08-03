package com.novabank.cuenta.application.port.out;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import reactor.core.publisher.Mono;

public interface MovimientoRegistradoPublisherPort {

    Mono<Void> publicar(MovimientoEventDTO movimiento);
}
