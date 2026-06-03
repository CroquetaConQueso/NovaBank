package com.novabank.operacion.event;

import com.novabank.events.operacion.OperacionSolicitadaEvent;
import com.novabank.operacion.exception.EventoNoPublicadoException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionEventPublisherTest {

    private final StreamBridge streamBridge = mock(StreamBridge.class);
    private final OperacionEventPublisher publisher = new OperacionEventPublisher(streamBridge);

    @Test
    void publicarOperacionSolicitadaCompletaCuandoStreamBridgeDevuelveTrue() {
        OperacionSolicitadaEvent event = solicitud("DEPOSITO");
        when(streamBridge.send(eq(OperacionEventPublisher.OPERACION_SOLICITADA_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        StepVerifier.create(publisher.publicarOperacionSolicitada(event, 10L))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<OperacionSolicitadaEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<OperacionSolicitadaEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(OperacionEventPublisher.OPERACION_SOLICITADA_BINDING), captor.capture());

        Message<OperacionSolicitadaEvent> message = captor.getValue();
        assertThat(message.getPayload()).isEqualTo(event);
        assertThat(new String((byte[]) message.getHeaders().get(KafkaHeaders.KEY), StandardCharsets.UTF_8))
                .isEqualTo("10");
    }

    @Test
    void publicarOperacionSolicitadaEmiteErrorCuandoStreamBridgeDevuelveFalse() {
        when(streamBridge.send(eq(OperacionEventPublisher.OPERACION_SOLICITADA_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        StepVerifier.create(publisher.publicarOperacionSolicitada(solicitud("RETIRADA"), 10L))
                .expectError(EventoNoPublicadoException.class)
                .verify();
    }

    private OperacionSolicitadaEvent solicitud(String tipoOperacion) {
        return new OperacionSolicitadaEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-03T10:15:30Z"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                tipoOperacion,
                tipoOperacion.equals("RETIRADA") ? 10L : null,
                tipoOperacion.equals("DEPOSITO") ? 10L : null,
                new BigDecimal("50.00"),
                "EUR"
        );
    }
}
