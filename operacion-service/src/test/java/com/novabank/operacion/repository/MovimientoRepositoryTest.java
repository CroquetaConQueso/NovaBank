package com.novabank.operacion.repository;

import com.novabank.operacion.model.Movimiento;
import com.novabank.operacion.model.TipoMovimiento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MovimientoRepositoryTest {

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Test
    void guardaYBuscaMovimientosPorCuentaYRangoDeFecha() {
        Movimiento movimiento = new Movimiento();
        movimiento.setCuentaId(10L);
        movimiento.setNumeroCuenta("ES91210000000000000001");
        movimiento.setTipo(TipoMovimiento.DEPOSITO);
        movimiento.setCantidad(new BigDecimal("50.00"));
        movimiento.setFecha(LocalDateTime.of(2026, 1, 15, 10, 30));

        Movimiento guardado = movimientoRepository.save(movimiento);

        assertThat(guardado.getId()).isNotNull();
        assertThat(movimientoRepository.findByCuentaIdOrderByFechaDesc(10L)).hasSize(1);
        assertThat(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                10L,
                LocalDateTime.of(2026, 1, 15, 0, 0),
                LocalDateTime.of(2026, 1, 15, 23, 59)
        )).hasSize(1);
    }
}
