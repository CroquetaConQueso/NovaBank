package com.novabank.operacion.event;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.operacion.service.OperacionAsincronaEstadoService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionResultadoEventConsumerConfigTest {

    private final OperacionAsincronaEstadoService estadoService = mock(OperacionAsincronaEstadoService.class);
    private final OperacionResultadoEventConsumerConfig config = new OperacionResultadoEventConsumerConfig();

    @Test
    void consumirOperacionCompletadaActualizaEstado() {
        when(estadoService.marcarCompletada(any(OperacionCompletadaEvent.class))).thenReturn(Mono.empty());
        Consumer<Flux<org.springframework.messaging.Message<OperacionCompletadaEvent>>> consumer =
                config.consumirOperacionCompletada(estadoService);

        OperacionCompletadaEvent event = completada();
        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        verify(estadoService, timeout(1000)).marcarCompletada(event);
    }

    @Test
    void consumirOperacionFallidaActualizaEstado() {
        when(estadoService.marcarFallida(any(OperacionFallidaEvent.class))).thenReturn(Mono.empty());
        Consumer<Flux<org.springframework.messaging.Message<OperacionFallidaEvent>>> consumer =
                config.consumirOperacionFallida(estadoService);

        OperacionFallidaEvent event = fallida();
        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        verify(estadoService, timeout(1000)).marcarFallida(event);
    }

    @Test
    void consumirOperacionInexistenteNoRompeElStream() {
        when(estadoService.marcarFallida(any(OperacionFallidaEvent.class))).thenReturn(Mono.empty());
        Consumer<Flux<org.springframework.messaging.Message<OperacionFallidaEvent>>> consumer =
                config.consumirOperacionFallida(estadoService);

        consumer.accept(Flux.just(MessageBuilder.withPayload(fallida()).build()));

        verify(estadoService, timeout(1000)).marcarFallida(any(OperacionFallidaEvent.class));
    }

    private OperacionCompletadaEvent completada() {
        return new OperacionCompletadaEvent(
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.now(),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "DEPOSITO",
                null,
                new BigDecimal("50.00"),
                "EUR"
        );
    }

    private OperacionFallidaEvent fallida() {
        return new OperacionFallidaEvent(
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.now(),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "RETIRADA",
                "SALDO_INSUFICIENTE",
                "Saldo insuficiente"
        );
    }
}
