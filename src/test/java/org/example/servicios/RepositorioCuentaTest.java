package org.example.servicios;

import org.example.modelos.Cliente;
import org.example.modelos.Cuenta;
import org.example.repositorio.RepositorioCuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase RepositorioCuenta.
 *
 * Verifica el correcto almacenamiento y recuperación
 * de cuentas bancarias en memoria.
 */
class RepositorioCuentaTest {

    private RepositorioCuenta repositorio;
    private Cliente cliente;

    @BeforeEach
    void setup() {
        repositorio = new RepositorioCuenta();

        cliente = new Cliente("Carlos", "Torres", "12345678A",
                "carlos@email.com", 600123123, LocalDateTime.now());
    }

    /**
     * Verifica que una cuenta se guarda correctamente
     * y puede recuperarse mediante su número de cuenta.
     */
    @Test
    void guardarCuenta_debePermitirBuscarPorNumero() {

        Cuenta cuenta = new Cuenta(cliente, "ES12345678901234567890", new BigDecimal("100"),
                LocalDateTime.now());

        repositorio.guardarCuenta(cuenta);

        Cuenta resultado = repositorio.buscarNumeroCuenta("ES12345678901234567890");

        assertNotNull(resultado);
        assertEquals("ES12345678901234567890", resultado.getNumeroCuenta());
    }

    /**
     * Verifica que si se guarda una cuenta con el mismo número,
     * el registro anterior es sobrescrito (comportamiento de HashMap).
     */
    @Test
    void guardarCuenta_conMismoNumero_debeSobrescribir() {

        Cuenta cuenta1 = new Cuenta(cliente, "ES12345678901234567890", new BigDecimal("100"),
                LocalDateTime.now());

        Cuenta cuenta2 = new Cuenta(cliente, "ES12345678901234567890", new BigDecimal("500"),
                LocalDateTime.now());

        repositorio.guardarCuenta(cuenta1);
        repositorio.guardarCuenta(cuenta2);

        Cuenta resultado = repositorio.buscarNumeroCuenta("ES12345678901234567890");

        assertEquals(new BigDecimal("500"), resultado.getSaldoCuenta());
    }

    /**
     * Verifica que buscarNumeroCuenta devuelve null
     * cuando no existe ninguna cuenta con el número indicado.
     */
    @Test
    void buscarNumeroCuenta_inexistente_debeRetornarNull() {

        Cuenta resultado = repositorio.buscarNumeroCuenta("ES00000000000000000000");

        assertNull(resultado);
    }

    /**
     * Verifica que listarCuentasCliente devuelve todas
     * las cuentas asociadas a un cliente concreto.
     */
    @Test
    void listarCuentasCliente_debeRetornarCuentasDelCliente() {

        Cuenta cuenta1 = new Cuenta(cliente, "ES11111111111111111111", new BigDecimal("100"),
                LocalDateTime.now());

        Cuenta cuenta2 = new Cuenta(cliente, "ES22222222222222222222", new BigDecimal("200"),
                LocalDateTime.now());

        repositorio.guardarCuenta(cuenta1);
        repositorio.guardarCuenta(cuenta2);

        List<Cuenta> resultado = repositorio.listarCuentasCliente(cliente.getIdCliente());

        assertEquals(2, resultado.size());
    }

    /**
     * Verifica que listarCuentasCliente devuelve
     * una lista vacía cuando el cliente no tiene cuentas.
     */
    @Test
    void listarCuentasCliente_sinResultados_debeRetornarListaVacia() {

        List<Cuenta> resultado = repositorio.listarCuentasCliente(cliente.getIdCliente());

        assertTrue(resultado.isEmpty());
    }

}
