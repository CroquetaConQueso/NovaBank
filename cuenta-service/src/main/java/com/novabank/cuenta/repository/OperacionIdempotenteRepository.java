package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.OperacionIdempotente;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OperacionIdempotenteRepository extends ReactiveCrudRepository<OperacionIdempotente, Long> {

    Mono<OperacionIdempotente> findByOperationId(String operationId);
}
