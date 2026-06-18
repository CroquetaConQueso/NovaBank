package com.novabank.cuenta.event;

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
            CuentaOperacionEventProcessor processor
    ) {
        return messages -> messages
                .concatMap(message -> processor.procesar(message)
                        .onErrorResume(error -> {
                            OperacionSolicitadaEvent event = message.getPayload();
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
                        }))
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("Error no recuperable en el consumidor de operaciones solicitadas", error)
                );
    }
}
