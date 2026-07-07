package com.novabank.cuenta.adapter.in.kafka;

import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaCommand;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaUseCase;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class CuentaOperacionEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(CuentaOperacionEventConsumerConfig.class);

    @Bean
    public Consumer<Flux<Message<OperacionSolicitadaEvent>>> procesarOperacion(
            ProcesarOperacionSolicitadaUseCase useCase
    ) {
        return messages -> messages
                .concatMap(message -> {
                    OperacionSolicitadaEvent event = message.getPayload();
                    return useCase.procesar(toCommand(event))
                            .onErrorResume(error -> {
                                log.error(
                                        "Error tecnico procesando resultado operationId={} tipoOperacion={} "
                                                + "cuentaOrigenId={} cuentaDestinoId={}",
                                        event.operationId(),
                                        event.tipoOperacion(),
                                        event.cuentaOrigenId(),
                                        event.cuentaDestinoId(),
                                        error
                                );
                                return reactor.core.publisher.Mono.empty();
                            });
                })
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("Error no recuperable en el consumidor de operaciones solicitadas", error)
                );
    }

    private ProcesarOperacionSolicitadaCommand toCommand(OperacionSolicitadaEvent event) {
        return new ProcesarOperacionSolicitadaCommand(
                event.eventId(),
                event.correlationId(),
                event.occurredAt(),
                event.operationId(),
                event.tipoOperacion(),
                event.cuentaOrigenId(),
                event.cuentaDestinoId(),
                event.importe(),
                event.moneda()
        );
    }
}
