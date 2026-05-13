package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.OperacionIdempotente;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OperacionIdempotenteRepository extends ReactiveCrudRepository<OperacionIdempotente, Long> {

    Mono<OperacionIdempotente> findByOperationId(String operationId);

    /**
     * Evita que dos peticiones concurrentes creen la misma operacion interna.
     */
    @Modifying
    @Query("""
            INSERT INTO operaciones_idempotentes
                (operation_id, request_hash, estado, fecha_creacion, fecha_actualizacion)
            VALUES
                (:operationId, :requestHash, 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (operation_id) DO NOTHING
            """)
    Mono<Integer> insertProcessingIfAbsent(
            @Param("operationId") String operationId,
            @Param("requestHash") String requestHash
    );
}
