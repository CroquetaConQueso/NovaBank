package com.novabank.cuenta.client;

import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteServiceClientFallbackFactoryTest {

    private final ClienteServiceClientFallbackFactory factory = new ClienteServiceClientFallbackFactory();

    @Test
    void fallbackConvierteFalloTecnicoEnServicioNoDisponible() {
        ClienteServiceClient fallback = factory.create(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> fallback.obtenerCliente(1L))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cliente-service no esta disponible");
    }

    @Test
    void fallbackPreservaClienteNoEncontradoMapeadoPorDecoder() {
        ClienteServiceClient fallback = factory.create(new ResourceNotFoundException("Cliente no encontrado"));

        assertThatThrownBy(() -> fallback.obtenerCliente(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado");
    }
}
