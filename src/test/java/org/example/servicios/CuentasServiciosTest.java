package org.example.servicios;

import org.example.modelos.Cliente;
import org.example.modelos.Cuenta;
import org.example.repositorio.RepositorioCliente;
import org.example.repositorio.RepositorioCuenta;
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

    // ===============================
    // BUSCAR NUMERO
    // ===============================

    @Test
    void buscarNumero_conFormatoInvalido_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.buscarNumero("123"));
    }

    @Test
    void buscarNumero_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES91210000000000000001")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.buscarNumero("ES91210000000000000001"));
    }

    @Test
    void buscarNumero_conCuentaValida_debeRetornarCuenta() {
        Cuenta cuenta = new Cuenta(cliente, "ES91210000000000000001",
                BigDecimal.ZERO, LocalDateTime.now());

        when(repoCuenta.buscarNumeroCuenta("ES91210000000000000001"))
                .thenReturn(cuenta);

        Cuenta resultado = cuentaServicio.buscarNumero("ES91210000000000000001");

        assertNotNull(resultado);
        assertEquals("ES91210000000000000001", resultado.getNumeroCuenta());
    }

    // ===============================
    // CREAR CUENTA
    // ===============================

    @Test
    void crearCuenta_conIdNegativo_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.crearCuenta(-1L));
    }

    @Test
    void crearCuenta_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.crearCuenta(1L));
    }

    @Test
    void crearCuenta_conClienteValido_debeCrearCuentaConSaldoCero() {
        when(repoCliente.buscarIdCliente(cliente.getIdCliente()))
                .thenReturn(cliente);

        Cuenta nuevaCuenta = cuentaServicio.crearCuenta(cliente.getIdCliente());

        assertNotNull(nuevaCuenta);
        assertEquals(BigDecimal.ZERO, nuevaCuenta.getSaldoCuenta());
        assertTrue(nuevaCuenta.getNumeroCuenta().startsWith("ES91210000"));

        verify(repoCuenta).guardarCuenta(any(Cuenta.class));
    }

    // ===============================
    // OBTENER TITULAR
    // ===============================

    @Test
    void obtenerTitular_conIdInvalido_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.obtenerTitular(0L));
    }

    @Test
    void obtenerTitular_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.obtenerTitular(1L));
    }

    @Test
    void obtenerTitular_conClienteValido_debeRetornarCliente() {
        when(repoCliente.buscarIdCliente(cliente.getIdCliente()))
                .thenReturn(cliente);

        Cliente resultado = cuentaServicio.obtenerTitular(cliente.getIdCliente());

        assertNotNull(resultado);
        assertEquals(cliente.getIdCliente(), resultado.getIdCliente());
    }

    // ===============================
    // OBTENER CUENTAS
    // ===============================

    @Test
    void obtenerCuentas_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> cuentaServicio.obtenerCuentas(1L));
    }

    @Test
    void obtenerCuentas_conClienteValido_debeRetornarLista() {
        when(repoCliente.buscarIdCliente(cliente.getIdCliente()))
                .thenReturn(cliente);

        when(repoCuenta.listarCuentasCliente(cliente.getIdCliente()))
                .thenReturn(List.of());

        List<Cuenta> cuentas = cuentaServicio.obtenerCuentas(cliente.getIdCliente());

        assertNotNull(cuentas);
        verify(repoCuenta).listarCuentasCliente(cliente.getIdCliente());
    }
}
