package com.novabank.notificacion.config;

import com.novabank.events.cliente.ClienteRegistradoEvent;
import com.novabank.notificacion.service.BienvenidaNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class ClienteRegistradoConsumerConfig {

    @Bean
    public Consumer<Flux<Message<ClienteRegistradoEvent>>> notificarBienvenida(
            BienvenidaNotificationService bienvenidaNotificationService
    ) {
        return messages -> messages
                .map(Message::getPayload)
                .doOnNext(bienvenidaNotificationService::registrarBienvenida)
                .subscribe();
    }
}
