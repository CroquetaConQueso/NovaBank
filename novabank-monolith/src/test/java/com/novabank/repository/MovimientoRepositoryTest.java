package com.novabank.repository;

import com.novabank.model.Cliente;
import com.novabank.model.Cuenta;
import com.novabank.model.Movimiento;
import com.novabank.model.TipoMovimiento;
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
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Test
    void guardaYBuscaMovimientosPorCuentaYRangoDeFecha() {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Marta")
                .apellidos("Sanchez Ruiz")
                .dni("11223344C")
                .email("marta.sanchez@example.com")
                .telefono("600555666")
                .build());

        Cuenta cuenta = cuentaRepository.save(Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta("ES99999999999999999999")
                .saldo(new BigDecimal("250.00"))
                .build());

        Movimiento movimiento = movimientoRepository.save(Movimiento.builder()
                .cuenta(cuenta)
                .tipo(TipoMovimiento.DEPOSITO)
                .cantidad(new BigDecimal("100.00"))
                .fecha(LocalDateTime.now())
                .build());

        assertThat(movimiento.getId()).isNotNull();
        assertThat(movimientoRepository.findByCuentaIdOrderByFechaDesc(cuenta.getId()))
                .containsExactly(movimiento);
        assertThat(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                cuenta.getId(),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        )).containsExactly(movimiento);
    }
}
