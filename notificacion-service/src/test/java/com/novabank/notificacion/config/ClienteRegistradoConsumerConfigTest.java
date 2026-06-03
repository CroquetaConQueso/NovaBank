package com.novabank.notificacion.config;

import com.novabank.events.cliente.ClienteRegistradoEvent;
import com.novabank.notificacion.service.BienvenidaNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class ClienteRegistradoConsumerConfigTest {

    private final BienvenidaNotificationService bienvenidaNotificationService = mock(BienvenidaNotificationService.class);
    private final ClienteRegistradoConsumerConfig config = new ClienteRegistradoConsumerConfig();

    @Test
    void notificarBienvenidaConsumesClienteRegistradoEvent() {
        ClienteRegistradoEvent event = new ClienteRegistradoEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-03T10:15:30Z"),
                1001L,
                "12345678Z",
                "Ana Garcia",
                "ana.garcia@example.com"
        );

        Consumer<Flux<org.springframework.messaging.Message<ClienteRegistradoEvent>>> consumer =
                config.notificarBienvenida(bienvenidaNotificationService);

        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        verify(bienvenidaNotificationService, timeout(1000)).registrarBienvenida(event);
    }
}
