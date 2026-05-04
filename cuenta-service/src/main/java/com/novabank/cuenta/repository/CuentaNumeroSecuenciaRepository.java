package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.CuentaNumeroSecuencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuentaNumeroSecuenciaRepository extends JpaRepository<CuentaNumeroSecuencia, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CuentaNumeroSecuencia s where s.id = :id")
    Optional<CuentaNumeroSecuencia> findByIdForUpdate(@Param("id") Long id);
}
