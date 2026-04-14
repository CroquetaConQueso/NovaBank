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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para ClienteServicio.
 */
@ExtendWith(MockitoExtension.class)
class ClienteServicioTest {

    @Mock
    private ClienteRepository repoCliente;

    @InjectMocks
    private ClienteServicio clienteServicio;

    private Cliente crearClienteExistente() {
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

    @Test
    void registrarCliente_datosValidos_debeGuardarCliente() {
        when(repoCliente.buscarDniCliente("12345678Z")).thenReturn(Optional.empty());
        when(repoCliente.buscarEmailCliente("carlos@example.com")).thenReturn(Optional.empty());
        when(repoCliente.buscarTelefonoCliente(612345678)).thenReturn(Optional.empty());

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);

        Cliente resultado = clienteServicio.registrarCliente(
                "Carlos",
                "Torres",
                "12345678z",
                "Carlos@Example.com",
                612345678
        );

        verify(repoCliente).anadirCliente(captor.capture());

        Cliente guardado = captor.getValue();

        assertNotNull(resultado);
        assertEquals("Carlos", guardado.getNombreCliente());
        assertEquals("Torres", guardado.getApellidosCliente());
        assertEquals("12345678Z", guardado.getDniNifCliente());
        assertEquals("carlos@example.com", guardado.getEmailCliente());
        assertEquals(612345678, guardado.getTelefonoCliente());
        assertNotNull(guardado.getFechaCreacionCliente());
    }

    @Test
    void registrarCliente_dniDuplicado_debeLanzarExcepcion() {
        when(repoCliente.buscarDniCliente("12345678Z")).thenReturn(Optional.of(crearClienteExistente()));

        assertThrows(
                DuplicateResourceException.class,
                () -> clienteServicio.registrarCliente(
                        "Carlos",
                        "Torres",
                        "12345678Z",
                        "otro@email.com",
                        611111111
                )
        );

        verify(repoCliente, never()).anadirCliente(any());
    }

    @Test
    void registrarCliente_emailInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> clienteServicio.registrarCliente(
                        "Carlos",
                        "Torres",
                        "12345678Z",
                        "email-invalido",
                        612345678
                )
        );

        verifyNoInteractions(repoCliente);
    }

    @Test
    void registrarCliente_telefonoInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> clienteServicio.registrarCliente(
                        "Carlos",
                        "Torres",
                        "12345678Z",
                        "carlos@example.com",
                        123
                )
        );

        verifyNoInteractions(repoCliente);
    }

    @Test
    void buscarIdCliente_inexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> clienteServicio.buscarIdCliente(99L)
        );
    }

    @Test
    void buscarDniCliente_invalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> clienteServicio.buscarDniCliente("123")
        );

        verifyNoInteractions(repoCliente);
    }

    @Test
    void listarClientes_debeRetornarLista() {
        when(repoCliente.obtenerClientes()).thenReturn(List.of(crearClienteExistente()));

        List<Cliente> resultado = clienteServicio.listarClientes();

        assertEquals(1, resultado.size());
        verify(repoCliente).obtenerClientes();
    }
}