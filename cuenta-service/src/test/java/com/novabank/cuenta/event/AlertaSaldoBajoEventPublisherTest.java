package com.novabank.cuenta.event;

import com.novabank.cuenta.adapter.out.kafka.AlertaSaldoBajoEventPublisher;
import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import com.novabank.events.alerta.AlertaSaldoBajoEvent;
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

class AlertaSaldoBajoEventPublisherTest {

    private final StreamBridge streamBridge = mock(StreamBridge.class);
    private final AlertaSaldoBajoEventPublisher publisher = new AlertaSaldoBajoEventPublisher(streamBridge);

    @Test
    void publicarCompletaCuandoStreamBridgeDevuelveTrue() {
        when(streamBridge.send(eq(AlertaSaldoBajoEventPublisher.ALERTA_SALDO_BAJO_BINDING),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        StepVerifier.create(publisher.publicar(evento("75.00"), new BigDecimal("100.00"))
                        .contextWrite(context -> context.put(
                                CorrelationIdSupport.CONTEXT_KEY,
                                "22222222-2222-2222-2222-222222222222"
                        )))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<AlertaSaldoBajoEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<AlertaSaldoBajoEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(AlertaSaldoBajoEventPublisher.ALERTA_SALDO_BAJO_BINDING), captor.capture());

        AlertaSaldoBajoEvent event = captor.getValue().getPayload();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(event.cuentaId()).isEqualTo(10L);
        assertThat(event.saldoActual()).isEqualByComparingTo("75.00");
        assertThat(event.umbral()).isEqualByComparingTo("100.00");
        assertThat(event.moneda()).isEqualTo("EUR");
        assertThat(captor.getValue().getHeaders().get(KafkaHeaders.KEY))
                .isEqualTo("10".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void publicarEmiteErrorCuandoStreamBridgeDevuelveFalse() {
        when(streamBridge.send(eq(AlertaSaldoBajoEventPublisher.ALERTA_SALDO_BAJO_BINDING),
                org.mockito.ArgumentMatchers.any())).thenReturn(false);

        StepVerifier.create(publisher.publicar(evento("75.00"), new BigDecimal("100.00")))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private MovimientoEventDTO evento(String saldoResultante) {
        return new MovimientoEventDTO(
                10L,
                null,
                "RETIRO",
                new BigDecimal("25.00"),
                new BigDecimal(saldoResultante),
                "Retiro interno",
                LocalDateTime.now(),
                "op-test"
        );
    }
}
