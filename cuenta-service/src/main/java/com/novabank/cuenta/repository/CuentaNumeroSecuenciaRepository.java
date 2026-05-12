package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.CuentaNumeroSecuencia;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CuentaNumeroSecuenciaRepository extends ReactiveCrudRepository<CuentaNumeroSecuencia, Long> {

    /**
     * El bloqueo pesimista queda expresado como SQL nativo hasta que se revise
     * la estrategia completa de concurrencia en la fase reactiva.
     */
    @Query("""
           SELECT id, next_value
           FROM account_number_sequence
           WHERE id = :id
           FOR UPDATE
           """)
    Mono<CuentaNumeroSecuencia> findByIdForUpdate(@Param("id") Long id);
}
