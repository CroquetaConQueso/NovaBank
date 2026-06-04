package com.novabank.notificacion.config;

import com.novabank.events.alerta.AlertaSaldoBajoEvent;
import com.novabank.notificacion.service.SaldoBajoNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class SaldoBajoConsumerConfig {

    @Bean
    public Consumer<Flux<Message<AlertaSaldoBajoEvent>>> notificarSaldoBajo(
            SaldoBajoNotificationService saldoBajoNotificationService
    ) {
        return messages -> messages
                .map(Message::getPayload)
                .doOnNext(saldoBajoNotificationService::registrarAlerta)
                .subscribe();
    }
}
