package com.novabank.operacion.adapter.in.kafka;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.operacion.application.port.in.ActualizarEstadoOperacionResultadoUseCase;
import com.novabank.operacion.application.port.in.ActualizarOperacionResultadoCommand;
import com.novabank.operacion.application.port.in.ActualizarOperacionResultadoResult;
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
            ActualizarEstadoOperacionResultadoUseCase useCase
    ) {
        return messages -> messages
                .concatMap(message -> {
                    OperacionCompletadaEvent event = message.getPayload();
                    return useCase.actualizar(new ActualizarOperacionResultadoCommand(
                                    event.operationId(),
                                    ActualizarOperacionResultadoCommand.Resultado.COMPLETADA,
                                    null,
                                    null
                            ))
                            .onErrorResume(error -> logYContinuar("completada", event.operationId(), error));
                })
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("Error no recuperable en el consumidor de operaciones completadas", error)
                );
    }

    @Bean
    public Consumer<Flux<Message<OperacionFallidaEvent>>> consumirOperacionFallida(
            ActualizarEstadoOperacionResultadoUseCase useCase
    ) {
        return messages -> messages
                .concatMap(message -> {
                    OperacionFallidaEvent event = message.getPayload();
                    return useCase.actualizar(new ActualizarOperacionResultadoCommand(
                                    event.operationId(),
                                    ActualizarOperacionResultadoCommand.Resultado.FALLIDA,
                                    event.codigoError(),
                                    event.motivo()
                            ))
                            .onErrorResume(error -> logYContinuar("fallida", event.operationId(), error));
                })
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("Error no recuperable en el consumidor de operaciones fallidas", error)
                );
    }

    private Mono<ActualizarOperacionResultadoResult> logYContinuar(String tipoResultado, Object operationId, Throwable error) {
        log.error(
                "Error procesando resultado de operacion {} operationId={}",
                tipoResultado,
                operationId,
                error
        );
        return Mono.empty();
    }
}
