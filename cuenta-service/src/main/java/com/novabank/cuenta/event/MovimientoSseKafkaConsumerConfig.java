package com.novabank.cuenta.event;

import com.novabank.events.movimiento.MovimientoRegistradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class MovimientoSseKafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(MovimientoSseKafkaConsumerConfig.class);

    @Bean
    public Consumer<Flux<Message<MovimientoRegistradoEvent>>> alimentarBusSse(
            MovimientoSseEventProcessor processor
    ) {
        return messages -> messages
                .concatMap(processor::procesar)
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("Error no recuperable en el consumidor SSE de movimientos", error)
                );
    }
}
