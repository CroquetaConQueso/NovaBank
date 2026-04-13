package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.exception.DuplicateResourceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para ClienteServicio.
 */
@ExtendWith(MockitoExtension.class)
class ClienteServicioTest {

    @Mock
    private ClienteRepository repoCliente;

    @InjectMocks
    private ClienteServicio clienteServicio;

    @Test
    void registrarCliente_datosValidos_debeGuardarCliente() {
        Cliente resultado = clienteServicio.registrarCliente(
                "Carlos",
                "Torres",
                "12345678Z",
                "carlos.torres@example.com",
                612345678
        );

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repoCliente).anadirCliente(captor.capture());

        Cliente clienteGuardado = captor.getValue();

        assertNotNull(resultado);
        assertEquals("Carlos", clienteGuardado.getNombreCliente());
        assertEquals("Torres", clienteGuardado.getApellidosCliente());
        assertEquals("12345678Z", clienteGuardado.getDniNifCliente());
        assertEquals("carlos.torres@example.com", clienteGuardado.getEmailCliente());
        assertEquals(612345678, clienteGuardado.getTelefonoCliente());
    }

    @Test
    void registrarCliente_dniDuplicado_debeLanzarExcepcion() {
        when(repoCliente.buscarDniCliente("12345678Z"))
                .thenReturn(Cliente.builder().nombreCliente("Otro").fechaCreacionCliente(LocalDateTime.now()).build());

        assertThrows(
                DuplicateResourceException.class,
                () -> clienteServicio.registrarCliente(
                        "Carlos",
                        "Torres",
                        "12345678Z",
                        "carlos.torres@example.com",
                        612345678
                )
        );

        verify(repoCliente, never()).anadirCliente(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registrarCliente_emailInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> clienteServicio.registrarCliente(
                        "Carlos",
                        "Torres",
                        "12345678Z",
                        "carlos..torres@-mail",
                        612345678
                )
        );

        verify(repoCliente, never()).anadirCliente(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registrarCliente_telefonoInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> clienteServicio.registrarCliente(
                        "Carlos",
                        "Torres",
                        "12345678Z",
                        "carlos.torres@example.com",
                        12345
                )
        );

        verify(repoCliente, never()).anadirCliente(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void buscarIdCliente_inexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(99L)).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> clienteServicio.buscarIdCliente(99L)
        );
    }

    @Test
    void buscarDniCliente_invalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> clienteServicio.buscarDniCliente("12A")
        );
    }
}