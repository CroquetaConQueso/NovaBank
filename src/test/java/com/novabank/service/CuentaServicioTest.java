package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.service.strategy.GeneradorNumeroCuentaStrategy;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;

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

    private Cuenta crearCuenta(Cliente cliente) {
        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES91210000123456789012")
                .saldoCuenta(BigDecimal.ZERO)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuenta.setIdCuenta(1L);
        return cuenta;
    }

    @Test
    void buscarNumero_conNumeroValidoYCuentaExistente_debeRetornarCuenta() {
        Cliente cliente = crearCliente();
        Cuenta cuenta = crearCuenta(cliente);

        when(repoCuenta.buscarNumeroCuenta("ES91210000123456789012"))
                .thenReturn(Optional.of(cuenta));

        Cuenta resultado = cuentaServicio.buscarNumero("ES91210000123456789012");

        assertEquals("ES91210000123456789012", resultado.getNumeroCuenta());
    }

    @Test
    void buscarNumero_conNumeroInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.buscarNumero("CUENTA_INVALIDA")
        );

        verify(repoCuenta, never()).buscarNumeroCuenta(anyString());
    }

    @Test
    void buscarNumero_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES91210000123456789012"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.buscarNumero("ES91210000123456789012")
        );
    }

    @Test
    void obtenerTitular_conIdValidoYClienteExistente_debeRetornarCliente() {
        Cliente cliente = crearCliente();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));

        Cliente resultado = cuentaServicio.obtenerTitular(1L);

        assertEquals(1L, resultado.getIdCliente());
        assertEquals("Carlos", resultado.getNombreCliente());
    }

    @Test
    void obtenerTitular_conIdNulo_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.obtenerTitular(null)
        );

        verify(repoCliente, never()).buscarIdCliente(any());
    }

    @Test
    void obtenerTitular_conIdNegativo_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.obtenerTitular(-1L)
        );

        verify(repoCliente, never()).buscarIdCliente(any());
    }

    @Test
    void obtenerTitular_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.obtenerTitular(99L)
        );
    }

    @Test
    void obtenerCuentas_conClienteValido_debeRetornarListaDeCuentas() {
        Cliente cliente = crearCliente();
        Cuenta cuenta1 = crearCuenta(cliente);

        Cuenta cuenta2 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES91210000999999999999")
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));
        when(repoCuenta.listarCuentasCliente(1L))
                .thenReturn(List.of(cuenta1, cuenta2));

        List<Cuenta> resultado = cuentaServicio.obtenerCuentas(1L);

        assertEquals(2, resultado.size());
        verify(repoCuenta).listarCuentasCliente(1L);
    }

    @Test
    void obtenerCuentas_conClienteSinCuentas_debeRetornarListaVacia() {
        Cliente cliente = crearCliente();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));
        when(repoCuenta.listarCuentasCliente(1L))
                .thenReturn(List.of());

        List<Cuenta> resultado = cuentaServicio.obtenerCuentas(1L);

        assertEquals(0, resultado.size());
    }

    @Test
    void crearCuenta_conClienteValido_debeCrearCuentaConSaldoCero() {
        Cliente cliente = crearCliente();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));
        when(generadorNumeroCuentaStrategy.generarNumeroCuenta())
                .thenReturn("ES91210000123456789012");

        Cuenta resultado = cuentaServicio.crearCuenta(1L);

        assertEquals("ES91210000123456789012", resultado.getNumeroCuenta());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.getSaldoCuenta()));
        assertEquals(cliente.getIdCliente(), resultado.getDueñoCuenta().getIdCliente());

        ArgumentCaptor<Cuenta> captor = ArgumentCaptor.forClass(Cuenta.class);
        verify(repoCuenta).guardarCuenta(captor.capture());

        Cuenta cuentaGuardada = captor.getValue();
        assertEquals("ES91210000123456789012", cuentaGuardada.getNumeroCuenta());
        assertEquals(0, BigDecimal.ZERO.compareTo(cuentaGuardada.getSaldoCuenta()));
        assertEquals(cliente.getIdCliente(), cuentaGuardada.getDueñoCuenta().getIdCliente());
    }

    @Test
    void crearCuenta_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.crearCuenta(1L)
        );

        verify(generadorNumeroCuentaStrategy, never()).generarNumeroCuenta();
        verify(repoCuenta, never()).guardarCuenta(any());
    }
}