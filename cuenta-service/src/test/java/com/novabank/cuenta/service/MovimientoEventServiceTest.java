package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MovimientoEventServiceTest {

    private final MovimientoEventService movimientoEventService = new MovimientoEventService();

    @Test
    void publicarMovimientoPermiteRecibirloEnStreamDeCuenta() {
        StepVerifier.create(movimientoEventService.streamDeCuenta(1L))
                .then(() -> movimientoEventService.publicar(evento(1L, "DEPOSITO")))
                .expectNextMatches(evento -> evento.cuentaId().equals(1L)
                        && evento.tipo().equals("DEPOSITO")
                        && evento.monto().compareTo(new BigDecimal("25.00")) == 0)
                .thenCancel()
                .verify();
    }

    @Test
    void streamDeCuentaFiltraEventosDeOtrasCuentas() {
        StepVerifier.create(movimientoEventService.streamDeCuenta(2L))
                .then(() -> movimientoEventService.publicar(evento(1L, "DEPOSITO")))
                .then(() -> movimientoEventService.publicar(evento(2L, "RETIRO")))
                .expectNextMatches(evento -> evento.cuentaId().equals(2L)
                        && evento.tipo().equals("RETIRO"))
                .thenCancel()
                .verify();
    }

    @Test
    void cuentaInvalidaDevuelveError() {
        StepVerifier.create(movimientoEventService.streamDeCuenta(0L))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void consumidorSinDemandaDescartaEventoPorBackpressure() {
        StepVerifier.create(movimientoEventService.streamDeCuenta(1L), 0)
                .then(() -> movimientoEventService.publicar(evento(1L, "DEPOSITO")))
                .thenAwait(Duration.ofMillis(50))
                .thenCancel()
                .verify();

        assertThat(movimientoEventService.eventosDescartados()).isEqualTo(1);
    }

    private MovimientoEventDTO evento(Long cuentaId, String tipo) {
        return new MovimientoEventDTO(
                cuentaId,
                null,
                tipo,
                new BigDecimal("25.00"),
                new BigDecimal("100.00"),
                "Evento de prueba",
                LocalDateTime.now(),
                "op-test"
        );
    }
}
