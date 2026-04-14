package com.novabank.persistence.memory;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para RepositorioMovimiento en memoria.
 */
class RepositorioMovimientoTest {

    private RepositorioMovimiento repositorio;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioMovimiento();

        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678Z")
                .emailCliente("carlos@example.com")
                .telefonoCliente(612345678)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();
        cliente.setIdCliente(1L);

        cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();
        cuenta.setIdCuenta(1L);
    }

    private Movimiento crearMovimiento(TipoMovimiento tipo, BigDecimal cantidad, LocalDateTime fecha) {
        return Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(tipo)
                .cantidadMovimiento(cantidad)
                .fechaCreacionMov(fecha)
                .build();
    }

    @Test
    void guardarMovimiento_debeAsignarIdYPersistirMovimiento() {
        Movimiento movimiento = crearMovimiento(
                TipoMovimiento.DEPOSITO,
                BigDecimal.valueOf(20),
                LocalDateTime.now()
        );

        repositorio.guardarMovimiento(movimiento);

        List<Movimiento> movimientos = repositorio.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertEquals(1, movimientos.size());
        assertNotNull(movimiento.getIdMovimiento());
        assertTrue(movimiento.getIdMovimiento() > 0);
    }

    @Test
    void obtenerMovimientosCuenta_debeRetornarMovimientosOrdenadosDescendentePorFecha() {
        Movimiento antiguo = crearMovimiento(
                TipoMovimiento.DEPOSITO,
                BigDecimal.valueOf(10),
                LocalDateTime.now().minusDays(1)
        );

        Movimiento reciente = crearMovimiento(
                TipoMovimiento.RETIRO,
                BigDecimal.valueOf(5),
                LocalDateTime.now()
        );

        repositorio.guardarMovimiento(antiguo);
        repositorio.guardarMovimiento(reciente);

        List<Movimiento> movimientos = repositorio.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertEquals(2, movimientos.size());
        assertEquals(TipoMovimiento.RETIRO, movimientos.get(0).getTipoMov());
        assertEquals(TipoMovimiento.DEPOSITO, movimientos.get(1).getTipoMov());
    }

    @Test
    void obtenerMovimientosCuenta_sinResultados_debeRetornarListaVacia() {
        List<Movimiento> movimientos = repositorio.obtenerMovimientosCuenta("ES00000000000000000000");

        assertTrue(movimientos.isEmpty());
    }

    @Test
    void obtenerMovimientosFecha_debeFiltrarPorRango() {
        Movimiento fueraDeRango = crearMovimiento(
                TipoMovimiento.DEPOSITO,
                BigDecimal.valueOf(10),
                LocalDateTime.now().minusDays(5)
        );

        Movimiento dentroDeRango = crearMovimiento(
                TipoMovimiento.RETIRO,
                BigDecimal.valueOf(5),
                LocalDateTime.now()
        );

        repositorio.guardarMovimiento(fueraDeRango);
        repositorio.guardarMovimiento(dentroDeRango);

        List<Movimiento> movimientos = repositorio.obtenerMovimientosFecha(
                cuenta.getNumeroCuenta(),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertEquals(1, movimientos.size());
        assertEquals(TipoMovimiento.RETIRO, movimientos.get(0).getTipoMov());
    }

    @Test
    void obtenerMovimientosFecha_sinResultados_debeRetornarListaVacia() {
        Movimiento movimiento = crearMovimiento(
                TipoMovimiento.DEPOSITO,
                BigDecimal.valueOf(10),
                LocalDateTime.now().minusDays(10)
        );

        repositorio.guardarMovimiento(movimiento);

        List<Movimiento> movimientos = repositorio.obtenerMovimientosFecha(
                cuenta.getNumeroCuenta(),
                LocalDate.now().minusDays(2),
                LocalDate.now().minusDays(1)
        );

        assertTrue(movimientos.isEmpty());
    }

    @Test
    void guardarMovimiento_variosMovimientos_debeAsignarIdsDiferentes() {
        Movimiento movimiento1 = crearMovimiento(
                TipoMovimiento.DEPOSITO,
                BigDecimal.valueOf(10),
                LocalDateTime.now().minusMinutes(1)
        );

        Movimiento movimiento2 = crearMovimiento(
                TipoMovimiento.RETIRO,
                BigDecimal.valueOf(5),
                LocalDateTime.now()
        );

        repositorio.guardarMovimiento(movimiento1);
        repositorio.guardarMovimiento(movimiento2);

        assertNotEquals(movimiento1.getIdMovimiento(), movimiento2.getIdMovimiento());
    }

    @Test
    void obtenerMovimientosCuenta_debeRetornarSoloLosDeLaCuentaSolicitada() {
        Cliente otroCliente = Cliente.builder()
                .nombreCliente("Ana")
                .apellidosCliente("Ruiz")
                .dniNifCliente("87654321B")
                .emailCliente("ana@email.com")
                .telefonoCliente(611111111)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();
        otroCliente.setIdCliente(2L);

        Cuenta otraCuenta = Cuenta.builder()
                .dueñoCuenta(otroCliente)
                .numeroCuenta("ES99999999999999999999")
                .saldoCuenta(BigDecimal.valueOf(50))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();
        otraCuenta.setIdCuenta(2L);

        Movimiento movimientoCuentaPrincipal = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.valueOf(20))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        Movimiento movimientoOtraCuenta = Movimiento.builder()
                .cuentaAsignada(otraCuenta)
                .tipoMov(TipoMovimiento.RETIRO)
                .cantidadMovimiento(BigDecimal.valueOf(7))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        repositorio.guardarMovimiento(movimientoCuentaPrincipal);
        repositorio.guardarMovimiento(movimientoOtraCuenta);

        List<Movimiento> movimientos = repositorio.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertEquals(1, movimientos.size());
        assertEquals(cuenta.getNumeroCuenta(), movimientos.get(0).getCuentaAsignada().getNumeroCuenta());
    }

    @Test
    void obtenerMovimientosFecha_enLosLimites_debeIncluirMovimientos() {
        LocalDate hoy = LocalDate.now();

        Movimiento movimiento = crearMovimiento(
                TipoMovimiento.DEPOSITO,
                BigDecimal.valueOf(12),
                hoy.atStartOfDay()
        );

        repositorio.guardarMovimiento(movimiento);

        List<Movimiento> movimientos = repositorio.obtenerMovimientosFecha(
                cuenta.getNumeroCuenta(),
                hoy,
                hoy
        );

        assertEquals(1, movimientos.size());
        assertEquals(TipoMovimiento.DEPOSITO, movimientos.get(0).getTipoMov());
    }
}