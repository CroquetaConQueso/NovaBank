package com.novabank.operacion.event;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.operacion.service.OperacionAsincronaEstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class OperacionResultadoEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(OperacionResultadoEventConsumerConfig.class);

    @Bean
    public Consumer<Flux<Message<OperacionCompletadaEvent>>> consumirOperacionCompletada(
            OperacionAsincronaEstadoService estadoService
    ) {
        return messages -> messages
                .concatMap(message -> estadoService.marcarCompletada(message.getPayload())
                        .onErrorResume(error -> logYContinuar("completada", message.getPayload().operationId(), error)))
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("Error no recuperable en el consumidor de operaciones completadas", error)
                );
    }

    @Bean
    public Consumer<Flux<Message<OperacionFallidaEvent>>> consumirOperacionFallida(
            OperacionAsincronaEstadoService estadoService
    ) {
        return messages -> messages
                .concatMap(message -> estadoService.marcarFallida(message.getPayload())
                        .onErrorResume(error -> logYContinuar("fallida", message.getPayload().operationId(), error)))
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("Error no recuperable en el consumidor de operaciones fallidas", error)
                );
    }

    private Mono<Void> logYContinuar(String tipoResultado, Object operationId, Throwable error) {
        log.error(
                "Error procesando resultado de operacion {} operationId={}",
                tipoResultado,
                operationId,
                error
        );
        return Mono.empty();
    }
}
