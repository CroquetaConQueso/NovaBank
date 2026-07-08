package com.novabank.operacion.application.port.out;

import com.novabank.operacion.domain.model.OperacionAsincrona;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OperacionAsincronaRepositoryPort {

    Mono<OperacionAsincrona> save(OperacionAsincrona operacion);

    Mono<OperacionAsincrona> findByOperationId(UUID operationId);
}
