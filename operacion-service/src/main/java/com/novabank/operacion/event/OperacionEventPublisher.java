package com.novabank.operacion.event;

import com.novabank.events.core.NovaBankTopics;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import com.novabank.operacion.exception.EventoNoPublicadoException;
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

import java.nio.charset.StandardCharsets;

@Component
public class OperacionEventPublisher {

    public static final String OPERACION_SOLICITADA_BINDING = "operacionSolicitada-out-0";

    private static final Logger log = LoggerFactory.getLogger(OperacionEventPublisher.class);

    private final StreamBridge streamBridge;

    public OperacionEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public Mono<Void> publicarOperacionSolicitada(OperacionSolicitadaEvent event, Long kafkaKey) {
        return Mono.fromCallable(() -> streamBridge.send(
                        OPERACION_SOLICITADA_BINDING,
                        message(event, kafkaKey)
                ))
                .flatMap(enviado -> enviado
                        ? Mono.<Void>empty()
                        : Mono.error(new EventoNoPublicadoException("No se pudo publicar OperacionSolicitadaEvent")))
                .doOnSuccess(ignored -> log.info(
                        "OperacionSolicitadaEvent publicado topic={} operationId={} tipoOperacion={} kafkaKey={}",
                        NovaBankTopics.OPERACIONES_SOLICITADAS,
                        event.operationId(),
                        event.tipoOperacion(),
                        kafkaKey
                ))
                .doOnError(error -> log.error(
                        "Error al publicar OperacionSolicitadaEvent topic={} operationId={} tipoOperacion={}",
                        NovaBankTopics.OPERACIONES_SOLICITADAS,
                        event.operationId(),
                        event.tipoOperacion(),
                        error
                ));
    }

    private Message<OperacionSolicitadaEvent> message(OperacionSolicitadaEvent event, Long kafkaKey) {
        MessageBuilder<OperacionSolicitadaEvent> builder = MessageBuilder.withPayload(event)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);

        if (kafkaKey != null) {
            builder.setHeader(KafkaHeaders.KEY, String.valueOf(kafkaKey).getBytes(StandardCharsets.UTF_8));
        }

        return builder.build();
    }
}
