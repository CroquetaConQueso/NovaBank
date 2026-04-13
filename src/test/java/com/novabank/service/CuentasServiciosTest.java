package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void buscarNumero_conFormatoInvalido_debeLanzarExcepcion() {
        assertThrows(ValidationException.class, () -> cuentaServicio.buscarNumero("123"));
    }

    @Test
    void buscarNumero_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890")).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.buscarNumero("ES12345678901234567890")
        );
    }

    @Test
    void buscarNumero_conCuentaValida_debeRetornarCuenta() {
        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678Z")
                .emailCliente("carlos@example.com")
                .telefonoCliente(612345678)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(BigDecimal.TEN)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890")).thenReturn(cuenta);

        Cuenta resultado = cuentaServicio.buscarNumero("es12345678901234567890");

        assertNotNull(resultado);
        assertEquals("ES12345678901234567890", resultado.getNumeroCuenta());
    }

    @Test
    void obtenerTitular_conIdInvalido_debeLanzarExcepcion() {
        assertThrows(ValidationException.class, () -> cuentaServicio.obtenerTitular(0L));
    }

    @Test
    void obtenerTitular_conClienteValido_debeRetornarCliente() {
        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678Z")
                .emailCliente("carlos@example.com")
                .telefonoCliente(612345678)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        when(repoCliente.buscarIdCliente(1L)).thenReturn(cliente);

        Cliente resultado = cuentaServicio.obtenerTitular(1L);

        assertNotNull(resultado);
        assertEquals("Carlos", resultado.getNombreCliente());
    }

    @Test
    void obtenerTitular_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(99L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> cuentaServicio.obtenerTitular(99L));
    }

    @Test
    void obtenerCuentas_conClienteValido_debeRetornarLista() {
        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678Z")
                .emailCliente("carlos@example.com")
                .telefonoCliente(612345678)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(BigDecimal.ZERO)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        when(repoCliente.buscarIdCliente(1L)).thenReturn(cliente);
        when(repoCuenta.listarCuentasCliente(cliente.getIdCliente())).thenReturn(List.of(cuenta));

        List<Cuenta> resultado = cuentaServicio.obtenerCuentas(1L);

        assertEquals(1, resultado.size());
    }

    @Test
    void obtenerCuentas_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(99L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> cuentaServicio.obtenerCuentas(99L));
    }

    @Test
    void crearCuenta_conIdNegativo_debeLanzarExcepcion() {
        assertThrows(ValidationException.class, () -> cuentaServicio.crearCuenta(-1L));
    }

    @Test
    void crearCuenta_conClienteInexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(99L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> cuentaServicio.crearCuenta(99L));
    }

    @Test
    void crearCuenta_conClienteValido_debeCrearCuentaConSaldoCero() {
        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678Z")
                .emailCliente("carlos@example.com")
                .telefonoCliente(612345678)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        when(repoCliente.buscarIdCliente(1L)).thenReturn(cliente);

        Cuenta cuenta = cuentaServicio.crearCuenta(1L);

        assertNotNull(cuenta);
        assertEquals(BigDecimal.ZERO, cuenta.getSaldoCuenta());
        verify(repoCuenta).guardarCuenta(cuenta);
    }
}