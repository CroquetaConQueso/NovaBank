package com.novabank.notificacion.config;

import com.novabank.events.alerta.AlertaSaldoBajoEvent;
import com.novabank.notificacion.service.SaldoBajoNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class SaldoBajoConsumerConfigTest {

    private final SaldoBajoNotificationService saldoBajoNotificationService = mock(SaldoBajoNotificationService.class);
    private final SaldoBajoConsumerConfig config = new SaldoBajoConsumerConfig();

    @Test
    void notificarSaldoBajoConsumeAlertaSaldoBajoEvent() {
        AlertaSaldoBajoEvent event = new AlertaSaldoBajoEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-04T10:15:30Z"),
                10L,
                new BigDecimal("75.00"),
                new BigDecimal("100.00"),
                "EUR"
        );

        Consumer<Flux<org.springframework.messaging.Message<AlertaSaldoBajoEvent>>> consumer =
                config.notificarSaldoBajo(saldoBajoNotificationService);

        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        verify(saldoBajoNotificationService, timeout(1000)).registrarAlerta(event);
    }
}
