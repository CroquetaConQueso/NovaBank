package com.novabank.cliente.event;

import com.novabank.cliente.exception.EventoNoPublicadoException;
import com.novabank.cliente.model.Cliente;
import com.novabank.cliente.tracing.CorrelationIdSupport;
import com.novabank.events.cliente.ClienteRegistradoEvent;
import com.novabank.events.core.NovaBankTopics;
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
public class ClienteEventPublisher {

    public static final String CLIENTE_REGISTRADO_BINDING = "clienteRegistrado-out-0";

    private static final Logger log = LoggerFactory.getLogger(ClienteEventPublisher.class);

    private final StreamBridge streamBridge;

    public ClienteEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public Mono<Void> publicarClienteRegistrado(Cliente cliente) {
        return Mono.deferContextual(contextView -> {
            ClienteRegistradoEvent event = new ClienteRegistradoEvent(
                    UUID.randomUUID(),
                    resolveCorrelationId(CorrelationIdSupport.fromContext(contextView)),
                    Instant.now(),
                    cliente.getId(),
                    cliente.getDni(),
                    cliente.getNombre(),
                    cliente.getEmail()
            );

            return Mono.fromCallable(() -> enviar(event))
                    .flatMap(enviado -> enviado
                            ? Mono.<Void>empty()
                            : Mono.error(new EventoNoPublicadoException("No se pudo publicar ClienteRegistradoEvent")))
                    .doOnSuccess(ignored -> log.info(
                            "ClienteRegistradoEvent publicado topic={} clienteId={} eventId={}",
                            NovaBankTopics.CLIENTES_REGISTRADOS,
                            event.clienteId(),
                            event.eventId()
                    ))
                    .doOnError(error -> log.error(
                            "Error al publicar ClienteRegistradoEvent topic={} clienteId={}",
                            NovaBankTopics.CLIENTES_REGISTRADOS,
                            event.clienteId(),
                            error
                    ));
        });
    }

    private boolean enviar(ClienteRegistradoEvent event) {
        Message<ClienteRegistradoEvent> message = MessageBuilder.withPayload(event)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .build();
        return streamBridge.send(CLIENTE_REGISTRADO_BINDING, message);
    }

    private UUID resolveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID();
        }

        try {
            return UUID.fromString(correlationId);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }
}
