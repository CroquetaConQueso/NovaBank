package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.NovaBankException;
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

    private Cliente crearClienteValido() {
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

    private Cuenta crearCuentaValida(Cliente cliente) {
        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES91210000000000000001")
                .saldoCuenta(BigDecimal.ZERO)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuenta.setIdCuenta(1L);
        return cuenta;
    }

    @Test
    void buscarNumero_conNumeroValidoYCuentaExistente_debeRetornarCuenta() {
        Cliente cliente = crearClienteValido();
        Cuenta cuenta = crearCuentaValida(cliente);

        when(repoCuenta.buscarNumeroCuenta("ES91210000000000000001"))
                .thenReturn(Optional.of(cuenta));

        Cuenta resultado = cuentaServicio.buscarNumero("ES91210000000000000001");

        assertEquals("ES91210000000000000001", resultado.getNumeroCuenta());
        verify(repoCuenta).buscarNumeroCuenta("ES91210000000000000001");
    }

    @Test
    void buscarNumero_conNumeroInvalido_debeLanzarValidationException() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.buscarNumero("CUENTA_INVALIDA")
        );

        verifyNoInteractions(repoCuenta);
    }

    @Test
    void buscarNumero_conCuentaInexistente_debeLanzarResourceNotFoundException() {
        when(repoCuenta.buscarNumeroCuenta("ES91210000000000000001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.buscarNumero("ES91210000000000000001")
        );

        verify(repoCuenta).buscarNumeroCuenta("ES91210000000000000001");
    }

    @Test
    void obtenerTitular_conIdValidoYClienteExistente_debeRetornarCliente() {
        Cliente cliente = crearClienteValido();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));

        Cliente resultado = cuentaServicio.obtenerTitular(1L);

        assertEquals(1L, resultado.getIdCliente());
        assertEquals("Carlos", resultado.getNombreCliente());
        verify(repoCliente).buscarIdCliente(1L);
    }

    @Test
    void obtenerTitular_conIdNulo_debeLanzarValidationException() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.obtenerTitular(null)
        );

        verifyNoInteractions(repoCliente);
    }

    @Test
    void obtenerTitular_conIdNegativo_debeLanzarValidationException() {
        assertThrows(
                ValidationException.class,
                () -> cuentaServicio.obtenerTitular(-1L)
        );

        verifyNoInteractions(repoCliente);
    }

    @Test
    void obtenerTitular_conClienteInexistente_debeLanzarResourceNotFoundException() {
        when(repoCliente.buscarIdCliente(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.obtenerTitular(99L)
        );

        verify(repoCliente).buscarIdCliente(99L);
    }

    @Test
    void obtenerCuentas_conClienteValido_debeRetornarListaDeCuentas() {
        Cliente cliente = crearClienteValido();
        Cuenta cuenta1 = crearCuentaValida(cliente);

        Cuenta cuenta2 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES91210000000000000002")
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));
        when(repoCuenta.listarCuentasCliente(1L))
                .thenReturn(List.of(cuenta1, cuenta2));

        List<Cuenta> resultado = cuentaServicio.obtenerCuentas(1L);

        assertEquals(2, resultado.size());
        verify(repoCliente).buscarIdCliente(1L);
        verify(repoCuenta).listarCuentasCliente(1L);
    }

    @Test
    void obtenerCuentas_conClienteSinCuentas_debeRetornarListaVacia() {
        Cliente cliente = crearClienteValido();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));
        when(repoCuenta.listarCuentasCliente(1L))
                .thenReturn(List.of());

        List<Cuenta> resultado = cuentaServicio.obtenerCuentas(1L);

        assertEquals(0, resultado.size());
        verify(repoCliente).buscarIdCliente(1L);
        verify(repoCuenta).listarCuentasCliente(1L);
    }

    @Test
    void crearCuenta_conClienteValido_debeCrearCuentaConSaldoCeroYUsarStrategy() {
        Cliente cliente = crearClienteValido();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));
        when(generadorNumeroCuentaStrategy.generarNumeroCuenta())
                .thenReturn("ES91210000000000000044");

        Cuenta resultado = cuentaServicio.crearCuenta(1L);

        assertEquals("ES91210000000000000044", resultado.getNumeroCuenta());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.getSaldoCuenta()));
        assertEquals(cliente.getIdCliente(), resultado.getDueñoCuenta().getIdCliente());

        verify(repoCliente).buscarIdCliente(1L);
        verify(generadorNumeroCuentaStrategy).generarNumeroCuenta();

        ArgumentCaptor<Cuenta> captor = ArgumentCaptor.forClass(Cuenta.class);
        verify(repoCuenta).guardarCuenta(captor.capture());

        Cuenta cuentaGuardada = captor.getValue();
        assertEquals("ES91210000000000000044", cuentaGuardada.getNumeroCuenta());
        assertEquals(0, BigDecimal.ZERO.compareTo(cuentaGuardada.getSaldoCuenta()));
        assertEquals(cliente.getIdCliente(), cuentaGuardada.getDueñoCuenta().getIdCliente());
    }

    @Test
    void crearCuenta_conClienteInexistente_debeLanzarResourceNotFoundException() {
        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cuentaServicio.crearCuenta(1L)
        );

        verify(repoCliente).buscarIdCliente(1L);
        verifyNoInteractions(generadorNumeroCuentaStrategy);
        verify(repoCuenta, never()).guardarCuenta(any());
    }

    @Test
    void crearCuenta_siLaStrategyFalla_debePropagarNovaBankException() {
        Cliente cliente = crearClienteValido();

        when(repoCliente.buscarIdCliente(1L))
                .thenReturn(Optional.of(cliente));
        when(generadorNumeroCuentaStrategy.generarNumeroCuenta())
                .thenThrow(new NovaBankException("No se pudo generar un número de cuenta único."));

        NovaBankException exception = assertThrows(
                NovaBankException.class,
                () -> cuentaServicio.crearCuenta(1L)
        );

        assertEquals("No se pudo generar un número de cuenta único.", exception.getMessage());
        verify(repoCliente).buscarIdCliente(1L);
        verify(generadorNumeroCuentaStrategy).generarNumeroCuenta();
        verify(repoCuenta, never()).guardarCuenta(any());
    }
}