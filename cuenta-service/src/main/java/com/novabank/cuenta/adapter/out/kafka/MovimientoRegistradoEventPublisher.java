package com.novabank.cuenta.adapter.out.kafka;

import com.novabank.cuenta.application.port.out.MovimientoRegistradoPublisherPort;
import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import com.novabank.events.core.NovaBankTopics;
import com.novabank.events.movimiento.MovimientoRegistradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class MovimientoRegistradoEventPublisher implements MovimientoRegistradoPublisherPort {

    public static final String MOVIMIENTO_REGISTRADO_BINDING = "movimientoRegistrado-out-0";
    private static final String MONEDA_LOCAL = "EUR";

    private static final Logger log = LoggerFactory.getLogger(MovimientoRegistradoEventPublisher.class);

    private final StreamBridge streamBridge;

    public MovimientoRegistradoEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public Mono<Void> publicar(MovimientoEventDTO movimiento) {
        return Mono.deferContextual(context -> publicar(movimiento, context));
    }

    private Mono<Void> publicar(MovimientoEventDTO movimiento, ContextView context) {
        String operationId = operationId(movimiento, context);
        MovimientoRegistradoEvent event = new MovimientoRegistradoEvent(
                UUID.randomUUID(),
                correlationId(context),
                Instant.now(),
                movimiento.movimientoId(),
                movimiento.cuentaId(),
                movimiento.tipo(),
                movimiento.monto(),
                movimiento.saldoResultante(),
                MONEDA_LOCAL
        );

        return Mono.fromCallable(() -> streamBridge.send(
                        MOVIMIENTO_REGISTRADO_BINDING,
                        message(event, movimiento.cuentaId())
                ))
                .flatMap(enviado -> enviado
                        ? Mono.<Void>empty()
                        : Mono.error(new IllegalStateException("No se pudo publicar MovimientoRegistradoEvent")))
                .doOnSuccess(ignored -> log.info(
                        "MovimientoRegistradoEvent publicado topic={} cuentaId={} operationId={} eventId={}",
                        NovaBankTopics.MOVIMIENTOS_REGISTRADOS,
                        event.cuentaId(),
                        operationId,
                        event.eventId()
                ))
                .doOnError(error -> log.error(
                        "Error al publicar MovimientoRegistradoEvent topic={} cuentaId={} operationId={}",
                        NovaBankTopics.MOVIMIENTOS_REGISTRADOS,
                        event.cuentaId(),
                        operationId,
                        error
                ));
    }

    private UUID correlationId(ContextView context) {
        String value = CorrelationIdSupport.fromContext(context);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            log.warn("correlationId invalido para MovimientoRegistradoEvent: {}", value);
            return null;
        }
    }

    private String operationId(MovimientoEventDTO movimiento, ContextView context) {
        if (movimiento.operationId() != null && !movimiento.operationId().isBlank()) {
            return movimiento.operationId();
        }
        Object value = context.hasKey(CorrelationIdSupport.OPERATION_ID_CONTEXT_KEY)
                ? context.get(CorrelationIdSupport.OPERATION_ID_CONTEXT_KEY)
                : null;
        if (value instanceof String operationId && !operationId.isBlank()) {
            return operationId;
        }
        return null;
    }

    private Message<MovimientoRegistradoEvent> message(MovimientoRegistradoEvent event, Long kafkaKey) {
        MessageBuilder<MovimientoRegistradoEvent> builder = MessageBuilder.withPayload(event)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);

        if (kafkaKey != null) {
            builder.setHeader(KafkaHeaders.KEY, String.valueOf(kafkaKey).getBytes(StandardCharsets.UTF_8));
        }

        return builder.build();
    }
}
