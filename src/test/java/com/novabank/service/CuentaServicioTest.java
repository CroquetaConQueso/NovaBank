package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CuentaServicio.
 */
@ExtendWith(MockitoExtension.class)
class CuentaServicioTest {

    @Mock
    private CuentaRepository repoCuenta;

    @Mock
    private ClienteRepository repoCliente;

    @InjectMocks
    private CuentaServicio cuentaServicio;

    private Cliente crearCliente() {
        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678Z")
                .emailCliente("carlos@example.com")
                .telefonoCliente(612345678)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        cliente.setIdCliente(1L);
        return cliente;
    }

    private Cuenta crearCuenta() {
        return Cuenta.builder()
                .dueñoCuenta(crearCliente())
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(BigDecimal.TEN)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();
    }

    @Test
    void buscarNumero_conCuentaValida_debeRetornarCuenta() {
        Cuenta cuenta = crearCuenta();

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));

        Cuenta resultado = cuentaServicio.buscarNumero("ES12345678901234567890");

        assertNotNull(resultado);
        assertEquals("ES12345678901234567890", resultado.getNumeroCuenta());
    }

    @Test
    void buscarNumero_conFormatoInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.buscarNumero("123")
        );

        verifyNoInteractions(repoCuenta);
    }

    @Test
    void buscarNumero_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.buscarNumero("ES12345678901234567890")
        );
    }

    @Test
    void obtenerTitular_conClienteValido_debeRetornarCliente() {
        Cliente cliente = crearCliente();

        when(repoCliente.buscarIdCliente(1L)).thenReturn(Optional.of(cliente));

        Cliente resultado = cuentaServicio.obtenerTitular(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdCliente());
    }

    @Test
    void obtenerTitular_conIdInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.obtenerTitular(-1L)
        );

        verifyNoInteractions(repoCliente);
    }

    @Test
    void obtenerTitular_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.obtenerTitular(1L)
        );
    }

    @Test
    void obtenerCuentas_conClienteValido_debeRetornarLista() {
        Cliente cliente = crearCliente();
        List<Cuenta> cuentas = List.of(crearCuenta());

        when(repoCliente.buscarIdCliente(1L)).thenReturn(Optional.of(cliente));
        when(repoCuenta.listarCuentasCliente(1L)).thenReturn(cuentas);

        List<Cuenta> resultado = cuentaServicio.obtenerCuentas(1L);

        assertEquals(1, resultado.size());
        verify(repoCuenta).listarCuentasCliente(1L);
    }

    @Test
    void obtenerCuentas_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.obtenerCuentas(1L)
        );

        verify(repoCuenta, never()).listarCuentasCliente(anyLong());
    }

    @Test
    void crearCuenta_conClienteValido_debeCrearCuentaConSaldoCero() {
        Cliente cliente = crearCliente();
        when(repoCliente.buscarIdCliente(1L)).thenReturn(Optional.of(cliente));

        ArgumentCaptor<Cuenta> captor = ArgumentCaptor.forClass(Cuenta.class);

        Cuenta cuentaCreada = cuentaServicio.crearCuenta(1L);

        verify(repoCuenta).guardarCuenta(captor.capture());

        Cuenta guardada = captor.getValue();

        assertNotNull(cuentaCreada);
        assertEquals(BigDecimal.ZERO, cuentaCreada.getSaldoCuenta());
        assertEquals(BigDecimal.ZERO, guardada.getSaldoCuenta());
        assertEquals(1L, guardada.getDueñoCuenta().getIdCliente());
        assertNotNull(guardada.getFechaCreacionCuenta());
        assertTrue(guardada.getNumeroCuenta().startsWith("ES"));
    }

    @Test
    void crearCuenta_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.crearCuenta(1L)
        );

        verify(repoCuenta, never()).guardarCuenta(any());
    }

    @Test
    void crearCuenta_conIdNegativo_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.crearCuenta(-1L)
        );

        verifyNoInteractions(repoCliente, repoCuenta);
    }
}