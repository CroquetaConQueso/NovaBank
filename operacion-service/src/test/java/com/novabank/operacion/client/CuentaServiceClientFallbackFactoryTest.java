package com.novabank.operacion.client;

import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaServiceClientFallbackFactoryTest {

    private final CuentaServiceClientFallbackFactory factory = new CuentaServiceClientFallbackFactory();

    @Test
    void fallbackConvierteFalloTecnicoEnServicioNoDisponible() {
        CuentaServiceClient fallback = factory.create(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> fallback.depositar(null, null))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cuenta-service no esta disponible");
    }

    @Test
    void fallbackPreservaValidacionRemotaMapeadaPorDecoder() {
        CuentaServiceClient fallback = factory.create(new RemoteValidationException("Saldo insuficiente"));

        assertThatThrownBy(() -> fallback.retirar(null, null))
                .isInstanceOf(RemoteValidationException.class)
                .hasMessageContaining("Saldo insuficiente");
    }
}
