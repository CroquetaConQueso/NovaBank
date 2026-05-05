package com.novabank.operacion.repository;

import com.novabank.operacion.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByCuentaIdOrderByFechaDesc(Long cuentaId);

    List<Movimiento> findByCuentaIdAndFechaBetweenOrderByFechaDesc(
            Long cuentaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );
}
