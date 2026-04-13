package com.novabank.persistence.jdbc;

import com.novabank.domain.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para ClienteRepositoryJdbc.
 *
 * Requiere base de datos PostgreSQL disponible y variables de entorno
 * configuradas para la conexión.
 */
class ClienteRepositoryJdbcIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;

    @BeforeEach
    void setUp() {
        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
    }

    @Test
    void anadirCliente_yBuscarPorDni_debePersistirYRecuperarCliente() {
        String sufijoUnico = String.valueOf(System.currentTimeMillis());

        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("1234567" + sufijoUnico.charAt(sufijoUnico.length() - 1) + "Z")
                .emailCliente("carlos" + sufijoUnico + "@example.com")
                .telefonoCliente(Integer.parseInt("6" + sufijoUnico.substring(sufijoUnico.length() - 8)))
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        clienteRepositoryJdbc.anadirCliente(cliente);

        assertTrue(cliente.getIdCliente() > 0);

        Cliente recuperado = clienteRepositoryJdbc.buscarDniCliente(cliente.getDniNifCliente());

        assertNotNull(recuperado);
        assertEquals(cliente.getDniNifCliente(), recuperado.getDniNifCliente());
        assertEquals(cliente.getEmailCliente(), recuperado.getEmailCliente());
    }
}

