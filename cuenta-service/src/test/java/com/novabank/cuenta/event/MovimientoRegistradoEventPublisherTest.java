package com.novabank.cuenta.event;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import com.novabank.events.movimiento.MovimientoRegistradoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovimientoRegistradoEventPublisherTest {

    private final StreamBridge streamBridge = mock(StreamBridge.class);
    private final MovimientoRegistradoEventPublisher publisher = new MovimientoRegistradoEventPublisher(streamBridge);

    @Test
    void publicarCompletaCuandoStreamBridgeDevuelveTrue() {
        when(streamBridge.send(eq(MovimientoRegistradoEventPublisher.MOVIMIENTO_REGISTRADO_BINDING),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        StepVerifier.create(publisher.publicar(evento(null))
                        .contextWrite(context -> context
                                .put(CorrelationIdSupport.CONTEXT_KEY, "22222222-2222-2222-2222-222222222222")
                                .put(CorrelationIdSupport.OPERATION_ID_CONTEXT_KEY, "op-ctx-1")))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<MovimientoRegistradoEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<MovimientoRegistradoEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(MovimientoRegistradoEventPublisher.MOVIMIENTO_REGISTRADO_BINDING), captor.capture());

        MovimientoRegistradoEvent event = captor.getValue().getPayload();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(event.cuentaId()).isEqualTo(10L);
        assertThat(event.tipoMovimiento()).isEqualTo("DEPOSITO");
        assertThat(event.importe()).isEqualByComparingTo("25.00");
        assertThat(event.saldoResultante()).isEqualByComparingTo("125.00");
        assertThat(event.moneda()).isEqualTo("EUR");
        assertThat(captor.getValue().getHeaders().get(KafkaHeaders.KEY))
                .isEqualTo("10".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void publicarUsaClaveKafkaPorCuentaIdAunqueOperationIdEsteEnDto() {
        when(streamBridge.send(eq(MovimientoRegistradoEventPublisher.MOVIMIENTO_REGISTRADO_BINDING),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        StepVerifier.create(publisher.publicar(evento("op-dto-1")))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<MovimientoRegistradoEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<MovimientoRegistradoEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(MovimientoRegistradoEventPublisher.MOVIMIENTO_REGISTRADO_BINDING), captor.capture());

        assertThat(captor.getValue().getHeaders().get(KafkaHeaders.KEY))
                .isEqualTo("10".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void publicarEmiteErrorCuandoStreamBridgeDevuelveFalse() {
        when(streamBridge.send(eq(MovimientoRegistradoEventPublisher.MOVIMIENTO_REGISTRADO_BINDING),
                org.mockito.ArgumentMatchers.any())).thenReturn(false);

        StepVerifier.create(publisher.publicar(evento(null)))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private MovimientoEventDTO evento(String operationId) {
        return new MovimientoEventDTO(
                10L,
                null,
                "DEPOSITO",
                new BigDecimal("25.00"),
                new BigDecimal("125.00"),
                "Deposito interno",
                LocalDateTime.now(),
                operationId
        );
    }
}
