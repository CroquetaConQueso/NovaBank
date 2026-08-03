package com.novabank.cuenta.adapter.in.kafka;

import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaCommand;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaResultado;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaUseCase;
import com.novabank.cuenta.application.port.out.OperacionResultadoPublisherPort;
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
            ProcesarOperacionSolicitadaUseCase useCase,
            OperacionResultadoPublisherPort operacionResultadoPublisherPort
    ) {
        return messages -> messages
                .concatMap(message -> {
                    OperacionSolicitadaEvent event = message.getPayload();
                    ProcesarOperacionSolicitadaCommand command = toCommand(event);
                    return useCase.procesar(command)
                            .flatMap(resultado -> publicarResultado(
                                    operacionResultadoPublisherPort,
                                    command,
                                    resultado
                            ))
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

    private reactor.core.publisher.Mono<ProcesarOperacionSolicitadaResultado> publicarResultado(
            OperacionResultadoPublisherPort operacionResultadoPublisherPort,
            ProcesarOperacionSolicitadaCommand command,
            ProcesarOperacionSolicitadaResultado resultado
    ) {
        if (resultado.estado() == ProcesarOperacionSolicitadaResultado.Estado.COMPLETADA) {
            return operacionResultadoPublisherPort.publicarCompletada(command)
                    .doOnSuccess(ignored -> log.info(
                            "Operacion aplicada y resultado COMPLETADA publicado operationId={} tipoOperacion={} "
                                    + "cuentaOrigenId={} cuentaDestinoId={}",
                            command.operationId(),
                            command.tipoOperacion(),
                            command.cuentaOrigenId(),
                            command.cuentaDestinoId()
                    ))
                    .thenReturn(resultado);
        }

        return operacionResultadoPublisherPort.publicarFallida(command, resultado.codigoError(), resultado.motivo())
                .doOnSuccess(ignored -> log.info(
                        "Operacion rechazada y resultado FALLIDA publicado operationId={} tipoOperacion={} "
                                + "cuentaOrigenId={} cuentaDestinoId={} codigoError={}",
                        command.operationId(),
                        command.tipoOperacion(),
                        command.cuentaOrigenId(),
                        command.cuentaDestinoId(),
                        resultado.codigoError()
                ))
                .thenReturn(resultado);
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
