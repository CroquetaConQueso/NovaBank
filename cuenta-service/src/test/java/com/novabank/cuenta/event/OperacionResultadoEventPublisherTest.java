package com.novabank.cuenta.event;

import com.novabank.cuenta.adapter.out.kafka.OperacionResultadoEventPublisher;
import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionResultadoEventPublisherTest {

    private final StreamBridge streamBridge = mock(StreamBridge.class);
    private final OperacionResultadoEventPublisher publisher = new OperacionResultadoEventPublisher(streamBridge);

    @Test
    void publicarCompletadaEnviaEventoAlBindingConfigurado() {
        OperacionSolicitadaEvent solicitud = solicitud("DEPOSITO");
        when(streamBridge.send(eq(OperacionResultadoEventPublisher.OPERACION_COMPLETADA_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        StepVerifier.create(publisher.publicarCompletada(solicitud))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<OperacionCompletadaEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<OperacionCompletadaEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(OperacionResultadoEventPublisher.OPERACION_COMPLETADA_BINDING), captor.capture());

        OperacionCompletadaEvent event = captor.getValue().getPayload();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(solicitud.correlationId());
        assertThat(event.operationId()).isEqualTo(solicitud.operationId());
        assertThat(event.tipoOperacion()).isEqualTo("DEPOSITO");
        assertThat(event.cuentaOrigenId()).isNull();
        assertThat(event.cuentaDestinoId()).isEqualTo(20L);
        assertThat(event.cuentaIdPrincipal()).isEqualTo(20L);
        assertThat(event.importe()).isEqualByComparingTo("25.00");
        assertThat(event.moneda()).isEqualTo("EUR");
    }

    @Test
    void publicarCompletadaDeRetiradaIncluyeCuentaOrigenComoPrincipal() {
        OperacionSolicitadaEvent solicitud = solicitud("RETIRADA");
        when(streamBridge.send(eq(OperacionResultadoEventPublisher.OPERACION_COMPLETADA_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        StepVerifier.create(publisher.publicarCompletada(solicitud))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<OperacionCompletadaEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<OperacionCompletadaEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(OperacionResultadoEventPublisher.OPERACION_COMPLETADA_BINDING), captor.capture());

        OperacionCompletadaEvent event = captor.getValue().getPayload();
        assertThat(event.cuentaOrigenId()).isEqualTo(10L);
        assertThat(event.cuentaDestinoId()).isNull();
        assertThat(event.cuentaIdPrincipal()).isEqualTo(10L);
    }

    @Test
    void publicarCompletadaDeTransferenciaIncluyeOrigenDestinoYOrigenComoPrincipal() {
        OperacionSolicitadaEvent solicitud = solicitud("TRANSFERENCIA");
        when(streamBridge.send(eq(OperacionResultadoEventPublisher.OPERACION_COMPLETADA_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        StepVerifier.create(publisher.publicarCompletada(solicitud))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<OperacionCompletadaEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<OperacionCompletadaEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(OperacionResultadoEventPublisher.OPERACION_COMPLETADA_BINDING), captor.capture());

        OperacionCompletadaEvent event = captor.getValue().getPayload();
        assertThat(event.cuentaOrigenId()).isEqualTo(10L);
        assertThat(event.cuentaDestinoId()).isEqualTo(20L);
        assertThat(event.cuentaIdPrincipal()).isEqualTo(10L);
    }

    @Test
    void publicarFallidaEnviaEventoAlBindingConfigurado() {
        OperacionSolicitadaEvent solicitud = solicitud("RETIRADA");
        when(streamBridge.send(eq(OperacionResultadoEventPublisher.OPERACION_FALLIDA_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        StepVerifier.create(publisher.publicarFallida(solicitud, "SALDO_INSUFICIENTE", "Saldo insuficiente"))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<OperacionFallidaEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<OperacionFallidaEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(OperacionResultadoEventPublisher.OPERACION_FALLIDA_BINDING), captor.capture());

        OperacionFallidaEvent event = captor.getValue().getPayload();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(solicitud.correlationId());
        assertThat(event.operationId()).isEqualTo(solicitud.operationId());
        assertThat(event.codigoError()).isEqualTo("SALDO_INSUFICIENTE");
        assertThat(event.motivo()).isEqualTo("Saldo insuficiente");
    }

    @Test
    void publicarCompletadaEmiteErrorCuandoStreamBridgeDevuelveFalse() {
        when(streamBridge.send(eq(OperacionResultadoEventPublisher.OPERACION_COMPLETADA_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        StepVerifier.create(publisher.publicarCompletada(solicitud("DEPOSITO")))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private OperacionSolicitadaEvent solicitud(String tipoOperacion) {
        return new OperacionSolicitadaEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-03T10:15:30Z"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                tipoOperacion,
                10L,
                20L,
                new BigDecimal("25.00"),
                "EUR"
        );
    }
}
