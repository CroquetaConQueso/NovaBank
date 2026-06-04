package com.novabank.cuenta.event;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import com.novabank.events.alerta.AlertaSaldoBajoEvent;
import com.novabank.events.core.NovaBankTopics;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class AlertaSaldoBajoEventPublisher {

    public static final String ALERTA_SALDO_BAJO_BINDING = "alertaSaldoBajo-out-0";
    private static final String MONEDA_LOCAL = "EUR";

    private static final Logger log = LoggerFactory.getLogger(AlertaSaldoBajoEventPublisher.class);

    private final StreamBridge streamBridge;

    public AlertaSaldoBajoEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public Mono<Void> publicar(MovimientoEventDTO movimiento, BigDecimal umbral) {
        return Mono.deferContextual(context -> publicar(movimiento, umbral, context));
    }

    private Mono<Void> publicar(MovimientoEventDTO movimiento, BigDecimal umbral, ContextView context) {
        AlertaSaldoBajoEvent event = new AlertaSaldoBajoEvent(
                UUID.randomUUID(),
                correlationId(context),
                Instant.now(),
                movimiento.cuentaId(),
                movimiento.saldoResultante(),
                umbral,
                MONEDA_LOCAL
        );

        return Mono.fromCallable(() -> streamBridge.send(
                        ALERTA_SALDO_BAJO_BINDING,
                        message(event, movimiento.cuentaId())
                ))
                .flatMap(enviado -> enviado
                        ? Mono.<Void>empty()
                        : Mono.error(new IllegalStateException("No se pudo publicar AlertaSaldoBajoEvent")))
                .doOnSuccess(ignored -> log.info(
                        "AlertaSaldoBajoEvent publicado topic={} cuentaId={} saldoActual={} umbral={} eventId={}",
                        NovaBankTopics.ALERTAS_SALDO_BAJO,
                        event.cuentaId(),
                        event.saldoActual(),
                        event.umbral(),
                        event.eventId()
                ))
                .doOnError(error -> log.error(
                        "Error al publicar AlertaSaldoBajoEvent topic={} cuentaId={} saldoActual={} umbral={}",
                        NovaBankTopics.ALERTAS_SALDO_BAJO,
                        event.cuentaId(),
                        event.saldoActual(),
                        event.umbral(),
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
            log.warn("correlationId invalido para AlertaSaldoBajoEvent: {}", value);
            return null;
        }
    }

    private Message<AlertaSaldoBajoEvent> message(AlertaSaldoBajoEvent event, Long kafkaKey) {
        MessageBuilder<AlertaSaldoBajoEvent> builder = MessageBuilder.withPayload(event)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);

        if (kafkaKey != null) {
            builder.setHeader(KafkaHeaders.KEY, String.valueOf(kafkaKey).getBytes(StandardCharsets.UTF_8));
        }

        return builder.build();
    }
}
