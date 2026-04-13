package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.persistence.memory.RepositorioCliente;
import com.novabank.persistence.memory.RepositorioCuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServicioTest {

    @Mock
    private RepositorioCuenta repoCuenta;

    @Mock
    private RepositorioCliente repoCliente;

    @InjectMocks
    private CuentaServicio cuentaServicio;

    private Cliente cliente;

    @BeforeEach
    void setup() {
        cliente = new Cliente(
                "Juan",
                "Perez",
                "12345678A",
                "juan@email.com",
                600123123,
                LocalDateTime.now()
        );
    }

    /**
     * Verifica que se lanza excepción cuando
     * el número de cuenta no cumple el formato IBAN esperado.
     */
    @Test
    void buscarNumero_conFormatoInvalido_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.buscarNumero("123"));
    }

    /**
     * Verifica que se lanza excepción cuando
     * la cuenta no existe en el repositorio.
     */
    @Test
    void buscarNumero_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES91210000000000000001"))
                .thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.buscarNumero("ES91210000000000000001"));
    }

    /**
     * Verifica que se retorna correctamente la cuenta
     * cuando existe y el formato es válido.
     */
    @Test
    void buscarNumero_conCuentaValida_debeRetornarCuenta() {
        Cuenta cuenta = new Cuenta(cliente,
                "ES91210000000000000001",
                BigDecimal.ZERO,
                LocalDateTime.now());

        when(repoCuenta.buscarNumeroCuenta("ES91210000000000000001"))
                .thenReturn(cuenta);

        Cuenta resultado =
                cuentaServicio.buscarNumero("ES91210000000000000001");

        assertNotNull(resultado);
        assertEquals("ES91210000000000000001",
                resultado.getNumeroCuenta());
    }

    /**
     * Verifica que no se permite crear una cuenta
     * con un identificador negativo.
     */
    @Test
    void crearCuenta_conIdNegativo_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.crearCuenta(-1L));
    }

    /**
     * Verifica que no se puede crear una cuenta
     * si el cliente no existe.
     */
    @Test
    void crearCuenta_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.crearCuenta(1L));
    }

    /**
     * Verifica que al crear una cuenta válida:
     * - Se inicializa con saldo cero
     * - Se genera un número IBAN correcto
     * - Se guarda en el repositorio
     */
    @Test
    void crearCuenta_conClienteValido_debeCrearCuentaConSaldoCero() {
        when(repoCliente.buscarIdCliente(cliente.getIdCliente()))
                .thenReturn(cliente);

        Cuenta nuevaCuenta =
                cuentaServicio.crearCuenta(cliente.getIdCliente());

        assertNotNull(nuevaCuenta);
        assertEquals(BigDecimal.ZERO,
                nuevaCuenta.getSaldoCuenta());
        assertTrue(nuevaCuenta.getNumeroCuenta()
                .startsWith("ES91210000"));

        verify(repoCuenta).guardarCuenta(any(Cuenta.class));
    }

    /**
     * Verifica que se lanza excepción cuando
     * el ID del cliente es inválido.
     */
    @Test
    void obtenerTitular_conIdInvalido_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.obtenerTitular(0L));
    }

    /**
     * Verifica que se lanza excepción cuando
     * el cliente no existe.
     */
    @Test
    void obtenerTitular_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.obtenerTitular(1L));
    }

    /**
     * Verifica que se retorna correctamente el cliente
     * cuando existe.
     */
    @Test
    void obtenerTitular_conClienteValido_debeRetornarCliente() {
        when(repoCliente.buscarIdCliente(cliente.getIdCliente()))
                .thenReturn(cliente);

        Cliente resultado =
                cuentaServicio.obtenerTitular(cliente.getIdCliente());

        assertNotNull(resultado);
        assertEquals(cliente.getIdCliente(),
                resultado.getIdCliente());
    }

    /**
     * Verifica que se lanza excepción cuando
     * se intentan obtener cuentas de un cliente inexistente.
     */
    @Test
    void obtenerCuentas_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.obtenerCuentas(1L));
    }

    /**
     * Verifica que se retorna correctamente la lista
     * de cuentas asociadas a un cliente válido.
     */
    @Test
    void obtenerCuentas_conClienteValido_debeRetornarLista() {
        when(repoCliente.buscarIdCliente(cliente.getIdCliente()))
                .thenReturn(cliente);

        when(repoCuenta.listarCuentasCliente(cliente.getIdCliente()))
                .thenReturn(List.of());

        List<Cuenta> cuentas =
                cuentaServicio.obtenerCuentas(cliente.getIdCliente());

        assertNotNull(cuentas);
        verify(repoCuenta)
                .listarCuentasCliente(cliente.getIdCliente());
    }
}
