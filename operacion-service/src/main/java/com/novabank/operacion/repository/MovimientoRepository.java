package com.novabank.operacion.repository;

import com.novabank.operacion.model.Movimiento;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface MovimientoRepository extends ReactiveCrudRepository<Movimiento, Long> {

    Flux<Movimiento> findByCuentaIdOrderByFechaDesc(Long cuentaId);

    Flux<Movimiento> findByCuentaIdAndFechaBetweenOrderByFechaDesc(
            Long cuentaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );
}
