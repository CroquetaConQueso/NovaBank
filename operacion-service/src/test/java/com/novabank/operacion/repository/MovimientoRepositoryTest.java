package com.novabank.operacion.repository;

import com.novabank.operacion.model.Movimiento;
import com.novabank.operacion.model.TipoMovimiento;
import com.novabank.operacion.testsupport.PostgresTestContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
class MovimientoRepositoryTest extends PostgresTestContainerSupport {

    @Autowired
    private MovimientoRepository movimientoRepository;

    @BeforeEach
    void setUp() {
        movimientoRepository.deleteAll().block();
    }

    @Test
    void guardaYBuscaMovimientosPorCuentaYRangoDeFecha() {
        Movimiento movimiento = movimiento(
                10L,
                TipoMovimiento.DEPOSITO,
                "50.00",
                LocalDateTime.of(2026, 1, 15, 10, 30)
        );

        StepVerifier.create(movimientoRepository.save(movimiento)
                        .flatMap(guardado -> movimientoRepository.findByCuentaIdOrderByFechaDesc(10L)
                                .collectList()
                                .zipWith(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                                                10L,
                                                LocalDateTime.of(2026, 1, 15, 0, 0),
                                                LocalDateTime.of(2026, 1, 15, 23, 59)
                                        )
                                        .collectList())
                                .map(tuple -> guardado)))
                .assertNext(guardado -> assertThat(guardado.getId()).isNotNull())
                .verifyComplete();
    }

    @Test
    void buscaMovimientosPorCuentaOrdenadosPorFechaDescendente() {
        Movimiento antiguo = movimiento(10L, TipoMovimiento.DEPOSITO, "10.00", LocalDateTime.of(2026, 1, 1, 10, 0));
        Movimiento reciente = movimiento(10L, TipoMovimiento.RETIRO, "5.00", LocalDateTime.of(2026, 1, 2, 10, 0));
        Movimiento otraCuenta = movimiento(11L, TipoMovimiento.DEPOSITO, "99.00", LocalDateTime.of(2026, 1, 3, 10, 0));

        StepVerifier.create(movimientoRepository.save(antiguo)
                        .then(movimientoRepository.save(reciente))
                        .then(movimientoRepository.save(otraCuenta))
                        .thenMany(movimientoRepository.findByCuentaIdOrderByFechaDesc(10L))
                        .map(Movimiento::getTipo))
                .expectNext(TipoMovimiento.RETIRO, TipoMovimiento.DEPOSITO)
                .verifyComplete();
    }

    @Test
    void rangoDeFechasExcluyeMovimientosFueraDelIntervalo() {
        Movimiento fuera = movimiento(10L, TipoMovimiento.DEPOSITO, "10.00", LocalDateTime.of(2026, 1, 1, 10, 0));
        Movimiento dentro = movimiento(10L, TipoMovimiento.RETIRO, "5.00", LocalDateTime.of(2026, 1, 5, 10, 0));

        StepVerifier.create(movimientoRepository.save(fuera)
                        .then(movimientoRepository.save(dentro))
                        .thenMany(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                                10L,
                                LocalDateTime.of(2026, 1, 5, 0, 0),
                                LocalDateTime.of(2026, 1, 5, 23, 59)
                        ))
                        .map(Movimiento::getTipo))
                .expectNext(TipoMovimiento.RETIRO)
                .verifyComplete();
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
