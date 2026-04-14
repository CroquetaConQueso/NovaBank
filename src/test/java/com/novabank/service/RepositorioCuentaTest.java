package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.persistence.memory.RepositorioCuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase RepositorioCuenta.
 */
class RepositorioCuentaTest {

    private RepositorioCuenta repositorio;
    private Cliente cliente;

    @BeforeEach
    void setup() {
        repositorio = new RepositorioCuenta();

        cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678A")
                .emailCliente("carlos@email.com")
                .telefonoCliente(600123123)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();
        cliente.setIdCliente(1L);
    }

    @Test
    void guardarCuenta_debePermitirBuscarPorNumero() {
        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(new BigDecimal("100"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repositorio.guardarCuenta(cuenta);

        Optional<Cuenta> resultado = repositorio.buscarNumeroCuenta("ES12345678901234567890");

        assertTrue(resultado.isPresent());
        assertEquals("ES12345678901234567890", resultado.get().getNumeroCuenta());
    }

    @Test
    void guardarCuenta_conMismoNumero_debeSobrescribir() {
        Cuenta cuenta1 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(new BigDecimal("100"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        Cuenta cuenta2 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(new BigDecimal("500"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repositorio.guardarCuenta(cuenta1);
        repositorio.guardarCuenta(cuenta2);

        Optional<Cuenta> resultado = repositorio.buscarNumeroCuenta("ES12345678901234567890");

        assertTrue(resultado.isPresent());
        assertEquals(new BigDecimal("500"), resultado.get().getSaldoCuenta());
    }

    @Test
    void buscarNumeroCuenta_inexistente_debeRetornarOptionalVacio() {
        Optional<Cuenta> resultado = repositorio.buscarNumeroCuenta("ES00000000000000000000");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarCuentasCliente_debeRetornarCuentasDelCliente() {
        Cuenta cuenta1 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES11111111111111111111")
                .saldoCuenta(new BigDecimal("100"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        Cuenta cuenta2 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES22222222222222222222")
                .saldoCuenta(new BigDecimal("200"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repositorio.guardarCuenta(cuenta1);
        repositorio.guardarCuenta(cuenta2);

        List<Cuenta> resultado = repositorio.listarCuentasCliente(cliente.getIdCliente());

        assertEquals(2, resultado.size());
    }

    @Test
    void listarCuentasCliente_sinResultados_debeRetornarListaVacia() {
        List<Cuenta> resultado = repositorio.listarCuentasCliente(cliente.getIdCliente());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void guardarCuenta_debeAsignarIdEnMemoria() {
        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(new BigDecimal("100"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repositorio.guardarCuenta(cuenta);

        assertTrue(cuenta.getIdCuenta() > 0);
    }

    @Test
    void actualizarSaldo_debeModificarElSaldoDeLaCuenta() {
        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(new BigDecimal("100"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repositorio.guardarCuenta(cuenta);

        repositorio.actualizarSaldo(null, "ES12345678901234567890", new BigDecimal("250"));

        Optional<Cuenta> resultado = repositorio.buscarNumeroCuenta("ES12345678901234567890");

        assertTrue(resultado.isPresent());
        assertEquals(new BigDecimal("250"), resultado.get().getSaldoCuenta());
    }

    @Test
    void buscarNumeroCuenta_conConnection_debeRetornarLaCuenta() {
        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(new BigDecimal("100"))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repositorio.guardarCuenta(cuenta);

        Optional<Cuenta> resultado = repositorio.buscarNumeroCuenta("ES12345678901234567890", null);

        assertTrue(resultado.isPresent());
        assertEquals("ES12345678901234567890", resultado.get().getNumeroCuenta());
    }
}