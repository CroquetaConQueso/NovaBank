package com.novabank.operacion.repository;

import com.novabank.operacion.model.OperacionPublicaIdempotente;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OperacionPublicaIdempotenteRepository
        extends ReactiveCrudRepository<OperacionPublicaIdempotente, Long> {

    Mono<OperacionPublicaIdempotente> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("""
            INSERT INTO operaciones_publicas_idempotentes
                (idempotency_key, request_hash, tipo_operacion, estado, fecha_creacion, fecha_actualizacion)
            VALUES
                (:idempotencyKey, :requestHash, :tipoOperacion, 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    Mono<Integer> insertProcessingIfAbsent(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash,
            @Param("tipoOperacion") String tipoOperacion
    );
}
