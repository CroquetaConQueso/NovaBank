package com.novabank.cuenta.adapter.out.kafka;

import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaCommand;
import com.novabank.cuenta.application.port.out.OperacionResultadoPublisherPort;
import com.novabank.events.core.NovaBankTopics;
import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
public class OperacionResultadoEventPublisher implements OperacionResultadoPublisherPort {

    public static final String OPERACION_COMPLETADA_BINDING = "operacionCompletada-out-0";
    public static final String OPERACION_FALLIDA_BINDING = "operacionFallida-out-0";

    private static final Logger log = LoggerFactory.getLogger(OperacionResultadoEventPublisher.class);

    private final StreamBridge streamBridge;

    public OperacionResultadoEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public Mono<Void> publicarCompletada(ProcesarOperacionSolicitadaCommand solicitud) {
        OperacionCompletadaEvent event = new OperacionCompletadaEvent(
                UUID.randomUUID(),
                solicitud.correlationId(),
                Instant.now(),
                solicitud.operationId(),
                solicitud.tipoOperacion(),
                cuentaOrigenId(solicitud),
                cuentaDestinoId(solicitud),
                cuentaIdPrincipal(solicitud),
                null,
                solicitud.importe(),
                solicitud.moneda()
        );

        return enviar(OPERACION_COMPLETADA_BINDING, event)
                .doOnSuccess(ignored -> log.info(
                        "OperacionCompletadaEvent publicado topic={} operationId={} eventId={}",
                        NovaBankTopics.OPERACIONES_COMPLETADAS,
                        event.operationId(),
                        event.eventId()
                ));
    }

    public Mono<Void> publicarCompletada(OperacionSolicitadaEvent solicitud) {
        return publicarCompletada(toCommand(solicitud));
    }

    @Override
    public Mono<Void> publicarFallida(ProcesarOperacionSolicitadaCommand solicitud, String codigoError, String motivo) {
        OperacionFallidaEvent event = new OperacionFallidaEvent(
                UUID.randomUUID(),
                solicitud.correlationId(),
                Instant.now(),
                solicitud.operationId(),
                solicitud.tipoOperacion(),
                codigoError,
                motivo
        );

        return enviar(OPERACION_FALLIDA_BINDING, event)
                .doOnSuccess(ignored -> log.info(
                        "OperacionFallidaEvent publicado topic={} operationId={} eventId={} codigoError={}",
                        NovaBankTopics.OPERACIONES_FALLIDAS,
                        event.operationId(),
                        event.eventId(),
                        event.codigoError()
                ));
    }

    public Mono<Void> publicarFallida(OperacionSolicitadaEvent solicitud, String codigoError, String motivo) {
        return publicarFallida(toCommand(solicitud), codigoError, motivo);
    }

    private Mono<Void> enviar(String bindingName, Object event) {
        return Mono.fromCallable(() -> streamBridge.send(bindingName, message(event)))
                .flatMap(enviado -> enviado
                        ? Mono.<Void>empty()
                        : Mono.error(new IllegalStateException("No se pudo publicar evento en binding " + bindingName)));
    }

    private Message<Object> message(Object event) {
        return MessageBuilder.withPayload(event)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .build();
    }

    private Long cuentaOrigenId(ProcesarOperacionSolicitadaCommand solicitud) {
        return switch (tipoNormalizado(solicitud)) {
            case "RETIRO", "RETIRADA", "TRANSFERENCIA" -> solicitud.cuentaOrigenId();
            default -> null;
        };
    }

    private Long cuentaDestinoId(ProcesarOperacionSolicitadaCommand solicitud) {
        return switch (tipoNormalizado(solicitud)) {
            case "DEPOSITO", "TRANSFERENCIA" -> solicitud.cuentaDestinoId();
            default -> null;
        };
    }

    private Long cuentaIdPrincipal(ProcesarOperacionSolicitadaCommand solicitud) {
        return switch (tipoNormalizado(solicitud)) {
            case "DEPOSITO" -> solicitud.cuentaDestinoId();
            case "RETIRO", "RETIRADA", "TRANSFERENCIA" -> solicitud.cuentaOrigenId();
            default -> solicitud.cuentaOrigenId() != null ? solicitud.cuentaOrigenId() : solicitud.cuentaDestinoId();
        };
    }

    private String tipoNormalizado(ProcesarOperacionSolicitadaCommand solicitud) {
        return solicitud.tipoOperacion() == null ? "" : solicitud.tipoOperacion().trim().toUpperCase(java.util.Locale.ROOT);
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
