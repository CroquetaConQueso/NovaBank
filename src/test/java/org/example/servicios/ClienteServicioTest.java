package org.example.servicios;

import org.example.modelos.Cliente;
import org.example.repositorio.RepositorioCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServicioTest {

    @Mock
    private RepositorioCliente repoCliente;

    @InjectMocks
    private ClienteServicio clienteServicio;

    private Cliente cliente;

    @BeforeEach
    void setup() {
        cliente = new Cliente("Juan", "Perez", "12345678A",
                "juan@email.com", 600123123, LocalDateTime.now());
    }

    /**
     * Verifica que un cliente con datos válidos:
     * - Supera todas las validaciones
     * - Se guarda correctamente en el repositorio
     */
    @Test
    void registrarCliente_datosValidos_debeGuardarCliente() {
        when(repoCliente.buscarDniCliente("12345678A")).thenReturn(null);
        when(repoCliente.buscarEmailCliente("juan@email.com")).thenReturn(null);
        when(repoCliente.buscarTelefonoCliente(600123123)).thenReturn(null);

        clienteServicio.registrarCliente("Juan", "Perez", "12345678A",
                "juan@email.com", 600123123);

        verify(repoCliente).anadirCliente(any(Cliente.class));
    }

    /**
     * Verifica que no se permite registrar un cliente
     * cuando el DNI ya existe en el sistema.
     */
    @Test
    void registrarCliente_dniDuplicado_debeLanzarExcepcion() {
        when(repoCliente.buscarDniCliente("12345678A")).thenReturn(cliente);

        assertThrows(IllegalArgumentException.class, () ->
                clienteServicio.registrarCliente("Juan", "Perez", "12345678A",
                        "juan@email.com", 600123123));
    }

    /**
     * Verifica que se lanza excepción cuando
     * el email no cumple el formato válido.
     */
    @Test
    void registrarCliente_emailInvalido_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                clienteServicio.registrarCliente("Juan", "Perez", "12345678A",
                        "emailinvalido", 600123123));
    }

    /**
     * Verifica que no se permite registrar un cliente
     * con un número de teléfono inválido.
     */
    @Test
    void registrarCliente_telefonoInvalido_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                clienteServicio.registrarCliente("Juan", "Perez", "12345678A",
                        "juan@email.com", -1));
    }

    /**
     * Verifica que se lanza excepción cuando
     * se busca un cliente por ID inexistente.
     */
    @Test
    void buscarIdCliente_inexistente_debeLanzarExcepcion() {
        when(repoCliente.buscarIdCliente(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> clienteServicio.buscarIdCliente(1L));
    }

    /**
     * Verifica que se lanza excepción cuando
     * el formato del DNI es inválido.
     */
    @Test
    void buscarDniCliente_invalido_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> clienteServicio.buscarDniCliente("123"));
    }
}
