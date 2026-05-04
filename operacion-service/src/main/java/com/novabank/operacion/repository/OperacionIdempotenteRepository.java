package com.novabank.operacion.repository;

import com.novabank.operacion.model.OperacionIdempotente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperacionIdempotenteRepository extends JpaRepository<OperacionIdempotente, Long> {

    Optional<OperacionIdempotente> findByIdempotencyKey(String idempotencyKey);
}
