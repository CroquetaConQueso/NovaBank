package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.Movimiento;
import com.novabank.cuenta.model.TipoMovimiento;
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
    private CuentaRepository cuentaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Test
    void guardaYBuscaMovimientosPorCuentaYRangoDeFecha() {
        Cuenta cuenta = cuentaRepository.save(cuenta());

        Movimiento movimiento = new Movimiento();
        movimiento.setCuenta(cuenta);
        movimiento.setTipo(TipoMovimiento.DEPOSITO);
        movimiento.setCantidad(new BigDecimal("100.00"));
        movimiento.setFecha(LocalDateTime.now());

        Movimiento guardado = movimientoRepository.save(movimiento);

        assertThat(guardado.getId()).isNotNull();
        assertThat(movimientoRepository.findByCuentaIdOrderByFechaDesc(cuenta.getId()))
                .containsExactly(guardado);
        assertThat(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                cuenta.getId(),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        )).containsExactly(guardado);
    }

    private Cuenta cuenta() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("ES99999999999999999999");
        cuenta.setClienteId(1L);
        cuenta.setSaldo(new BigDecimal("250.00"));
        return cuenta;
    }
}
