package com.novabank.notificacion.config;

import com.novabank.events.alerta.AlertaOperacionSospechosaEvent;
import com.novabank.notificacion.service.OperacionSospechosaNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class OperacionSospechosaConsumerConfigTest {

    private final OperacionSospechosaNotificationService notificationService =
            mock(OperacionSospechosaNotificationService.class);
    private final OperacionSospechosaConsumerConfig config = new OperacionSospechosaConsumerConfig();

    @Test
    void notificarOperacionSospechosaConsumeAlertaOperacionSospechosaEvent() {
        AlertaOperacionSospechosaEvent event = new AlertaOperacionSospechosaEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-04T10:15:30Z"),
                10L,
                "RETIRADA",
                6L,
                10L,
                "Mas de 5 retiradas en 10 minutos"
        );

        Consumer<Flux<org.springframework.messaging.Message<AlertaOperacionSospechosaEvent>>> consumer =
                config.notificarOperacionSospechosa(notificationService);

        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        verify(notificationService, timeout(1000)).registrarAlerta(event);
    }
}
