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

    @Test
    void buscaMovimientosPorCuentaOrdenadosPorFechaDescendente() {
        Movimiento antiguo = movimiento(10L, TipoMovimiento.DEPOSITO, "10.00", LocalDateTime.of(2026, 1, 1, 10, 0));
        Movimiento reciente = movimiento(10L, TipoMovimiento.RETIRO, "5.00", LocalDateTime.of(2026, 1, 2, 10, 0));
        Movimiento otraCuenta = movimiento(11L, TipoMovimiento.DEPOSITO, "99.00", LocalDateTime.of(2026, 1, 3, 10, 0));
        movimientoRepository.save(antiguo);
        movimientoRepository.save(reciente);
        movimientoRepository.save(otraCuenta);

        assertThat(movimientoRepository.findByCuentaIdOrderByFechaDesc(10L))
                .extracting(Movimiento::getTipo)
                .containsExactly(TipoMovimiento.RETIRO, TipoMovimiento.DEPOSITO);
    }

    @Test
    void rangoDeFechasExcluyeMovimientosFueraDelIntervalo() {
        Movimiento fuera = movimiento(10L, TipoMovimiento.DEPOSITO, "10.00", LocalDateTime.of(2026, 1, 1, 10, 0));
        Movimiento dentro = movimiento(10L, TipoMovimiento.RETIRO, "5.00", LocalDateTime.of(2026, 1, 5, 10, 0));
        movimientoRepository.save(fuera);
        movimientoRepository.save(dentro);

        assertThat(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                10L,
                LocalDateTime.of(2026, 1, 5, 0, 0),
                LocalDateTime.of(2026, 1, 5, 23, 59)
        ))
                .extracting(Movimiento::getTipo)
                .containsExactly(TipoMovimiento.RETIRO);
    }

    private Movimiento movimiento(Long cuentaId, TipoMovimiento tipo, String cantidad, LocalDateTime fecha) {
        Movimiento movimiento = new Movimiento();
        movimiento.setCuentaId(cuentaId);
        movimiento.setNumeroCuenta("ES91210000000000000001");
        movimiento.setTipo(tipo);
        movimiento.setCantidad(new BigDecimal(cantidad));
        movimiento.setFecha(fecha);
        return movimiento;
    }
}
