package com.novabank.notificacion.config;

import com.novabank.events.alerta.AlertaOperacionSospechosaEvent;
import com.novabank.notificacion.service.OperacionSospechosaNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class OperacionSospechosaConsumerConfig {

    @Bean
    public Consumer<Flux<Message<AlertaOperacionSospechosaEvent>>> notificarOperacionSospechosa(
            OperacionSospechosaNotificationService operacionSospechosaNotificationService
    ) {
        return messages -> messages
                .map(Message::getPayload)
                .doOnNext(operacionSospechosaNotificationService::registrarAlerta)
                .subscribe();
    }
}
