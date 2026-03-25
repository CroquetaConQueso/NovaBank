package org.example.servicios;

import org.example.modelos.Cliente;
import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.modelos.TipoMovimiento;
import org.example.repositorio.RepositorioMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase RepositorioMovimiento.
 *
 * Verifica el correcto almacenamiento, ordenación
 * y filtrado de movimientos financieros en memoria.
 */
class RepositorioMovimientoTest {

    private RepositorioMovimiento repositorio;
    private Cuenta cuenta;

    @BeforeEach
    void setup() {

        repositorio = new RepositorioMovimiento();

        Cliente cliente = new Cliente("Carlos", "Torres", "12345678A",
                "carlos@email.com", 600123123, LocalDateTime.now());

        cuenta = new Cuenta(cliente, "ES12345678901234567890", new BigDecimal("1000"), LocalDateTime.now());
    }

    /**
     * Verifica que un movimiento se almacena correctamente
     * y puede recuperarse mediante el número de cuenta.
     */
    @Test
    void guardarMovimiento_debeAlmacenarCorrectamente() {
        Movimiento movimiento = new Movimiento(cuenta, TipoMovimiento.DEPOSITO, new BigDecimal("100"), LocalDateTime.now());

        repositorio.guardarMovimiento(movimiento);

        List<Movimiento> resultado = repositorio.obtenerMovimientosCuenta("ES12345678901234567890");

        assertEquals(1, resultado.size());
    }

    /**
     * Verifica que no se permite duplicar movimientos
     * con el mismo identificador.
     */
    @Test
    void guardarMovimiento_conMismaId_noDebeDuplicar() {

        Movimiento movimiento = new Movimiento(cuenta, TipoMovimiento.DEPOSITO, new BigDecimal("100"), LocalDateTime.now()
        );

        repositorio.guardarMovimiento(movimiento);
        repositorio.guardarMovimiento(movimiento);

        List<Movimiento> resultado = repositorio.obtenerMovimientosCuenta("ES12345678901234567890");

        assertEquals(1, resultado.size());
    }

    /**
     * Verifica que los movimientos se devuelven
     * ordenados por fecha descendente.
     */
    @Test
    void obtenerMovimientosCuenta_debeOrdenarPorFechaDescendente() {

        Movimiento antiguo = new Movimiento(
                cuenta,
                TipoMovimiento.DEPOSITO,
                new BigDecimal("100"),
                LocalDateTime.now().minusDays(2)
        );

        Movimiento reciente = new Movimiento(
                cuenta,
                TipoMovimiento.DEPOSITO,
                new BigDecimal("200"),
                LocalDateTime.now()
        );

        repositorio.guardarMovimiento(antiguo);
        repositorio.guardarMovimiento(reciente);

        List<Movimiento> resultado = repositorio.obtenerMovimientosCuenta("ES12345678901234567890");

        assertEquals(2, resultado.size());
        assertTrue(resultado.get(0).getFechaCreacionMov()
                .isAfter(resultado.get(1).getFechaCreacionMov()));
    }

    /**
     * Verifica que obtenerMovimientosCuenta devuelve
     * una lista vacía cuando no existen movimientos.
     */
    @Test
    void obtenerMovimientosCuenta_sinResultados_debeRetornarListaVacia() {

        List<Movimiento> resultado = repositorio.obtenerMovimientosCuenta("ES12345678901234567890");

        assertTrue(resultado.isEmpty());
    }

    /**
     * Verifica que obtenerMovimientosFecha devuelve únicamente
     * los movimientos dentro del rango indicado (inclusive).
     */
    @Test
    void obtenerMovimientosFecha_debeFiltrarPorRango() {

        Movimiento mov1 = new Movimiento(
                cuenta,
                TipoMovimiento.DEPOSITO,
                new BigDecimal("100"),
                LocalDateTime.of(2026,3,1,10,0)
        );

        Movimiento mov2 = new Movimiento(
                cuenta,
                TipoMovimiento.DEPOSITO,
                new BigDecimal("200"),
                LocalDateTime.of(2026,3,15,10,0)
        );

        Movimiento mov3 = new Movimiento(
                cuenta,
                TipoMovimiento.DEPOSITO,
                new BigDecimal("300"),
                LocalDateTime.of(2026,4,1,10,0)
        );

        repositorio.guardarMovimiento(mov1);
        repositorio.guardarMovimiento(mov2);
        repositorio.guardarMovimiento(mov3);

        List<Movimiento> resultado = repositorio.obtenerMovimientosFecha(
                "ES12345678901234567890",
                LocalDate.of(2026,3,1),
                LocalDate.of(2026,3,31)
        );

        assertEquals(2, resultado.size());
    }

    /**
     * Verifica que obtenerMovimientosFecha devuelve
     * una lista vacía cuando no hay movimientos
     * dentro del rango indicado.
     */
    @Test
    void obtenerMovimientosFecha_sinCoincidencias_debeRetornarListaVacia() {

        Movimiento movimiento = new Movimiento(
                cuenta,
                TipoMovimiento.DEPOSITO,
                new BigDecimal("100"),
                LocalDateTime.of(2026,1,1,10,0)
        );

        repositorio.guardarMovimiento(movimiento);

        List<Movimiento> resultado = repositorio.obtenerMovimientosFecha(
                "ES12345678901234567890",
                LocalDate.of(2026,3,1),
                LocalDate.of(2026,3,31)
        );

        assertTrue(resultado.isEmpty());
    }

}

