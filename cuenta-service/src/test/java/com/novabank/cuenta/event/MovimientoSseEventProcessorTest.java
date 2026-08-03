package com.novabank.cuenta.event;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.service.MovimientoEventService;
import com.novabank.events.movimiento.MovimientoRegistradoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.support.MessageBuilder;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MovimientoSseEventProcessorTest {

    private final MovimientoEventService movimientoEventService = mock(MovimientoEventService.class);
    private final MovimientoSseEventProcessor processor = new MovimientoSseEventProcessor(movimientoEventService);

    @Test
    void procesarEventoValidoPublicaEnBusSse() {
        MovimientoRegistradoEvent event = evento(10L);

        StepVerifier.create(processor.procesar(MessageBuilder.withPayload(event).build()))
                .verifyComplete();

        ArgumentCaptor<MovimientoEventDTO> captor = ArgumentCaptor.forClass(MovimientoEventDTO.class);
        verify(movimientoEventService).publicar(captor.capture());

        MovimientoEventDTO dto = captor.getValue();
        assertThat(dto.cuentaId()).isEqualTo(10L);
        assertThat(dto.operationId()).isNull();
        assertThat(dto.tipo()).isEqualTo("DEPOSITO");
        assertThat(dto.monto()).isEqualByComparingTo("25.00");
        assertThat(dto.saldoResultante()).isEqualByComparingTo("125.00");
    }

    @Test
    void procesarEventoSinCuentaIdNoRompeElFlujoNiPublica() {
        MovimientoRegistradoEvent event = evento(null);

        StepVerifier.create(processor.procesar(MessageBuilder.withPayload(event).build()))
                .verifyComplete();

        verify(movimientoEventService, never()).publicar(org.mockito.ArgumentMatchers.any());
    }

    private MovimientoRegistradoEvent evento(Long cuentaId) {
        return new MovimientoRegistradoEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-03T10:15:30Z"),
                null,
                cuentaId,
                "DEPOSITO",
                new BigDecimal("25.00"),
                new BigDecimal("125.00"),
                "EUR"
        );
    }
}
