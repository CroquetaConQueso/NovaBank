package com.novabank.documento.adapter.in.kafka;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.documento.application.port.in.GenerarJustificanteOperacionCommand;
import com.novabank.documento.application.port.in.GenerarJustificanteOperacionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class OperacionCompletadaDocumentoConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(OperacionCompletadaDocumentoConsumerConfig.class);

    @Bean
    public Consumer<Flux<Message<OperacionCompletadaEvent>>> generarJustificanteOperacion(
            GenerarJustificanteOperacionUseCase useCase
    ) {
        return messages -> messages
                .concatMap(message -> generarJustificante(message.getPayload(), useCase))
                .subscribe();
    }

    private Mono<Void> generarJustificante(
            OperacionCompletadaEvent event,
            GenerarJustificanteOperacionUseCase useCase
    ) {
        if (event.cuentaIdPrincipal() == null) {
            log.error(
                    "OperacionCompletadaEvent sin cuentaIdPrincipal; no se genera justificante operationId={}",
                    event.operationId()
            );
            return Mono.empty();
        }

        return useCase.generar(toCommand(event))
                .doOnSuccess(documento -> log.info(
                        "Justificante generado para operacion={} cuentaId={} claveObjeto={}",
                        documento.operacionId(),
                        documento.cuentaId(),
                        documento.claveObjeto()
                ))
                .then()
                .onErrorResume(error -> {
                    log.error("Error generando justificante de operacion completada", error);
                    return Mono.empty();
                });
    }

    private GenerarJustificanteOperacionCommand toCommand(OperacionCompletadaEvent event) {
        return new GenerarJustificanteOperacionCommand(
                event.eventId(),
                event.correlationId(),
                event.occurredAt(),
                event.operationId(),
                event.tipoOperacion(),
                event.cuentaOrigenId(),
                event.cuentaDestinoId(),
                event.cuentaIdPrincipal(),
                event.movimientoId(),
                event.importe(),
                event.moneda()
        );
    }
}
