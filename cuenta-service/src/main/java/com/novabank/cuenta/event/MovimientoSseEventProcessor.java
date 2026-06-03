package com.novabank.cuenta.event;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.service.MovimientoEventService;
import com.novabank.events.movimiento.MovimientoRegistradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class MovimientoSseEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(MovimientoSseEventProcessor.class);

    private final MovimientoEventService movimientoEventService;

    public MovimientoSseEventProcessor(MovimientoEventService movimientoEventService) {
        this.movimientoEventService = movimientoEventService;
    }

    public Mono<Void> procesar(Message<MovimientoRegistradoEvent> message) {
        return Mono.fromRunnable(() -> publicarEnBus(message.getPayload()));
    }

    private void publicarEnBus(MovimientoRegistradoEvent event) {
        if (event == null || event.cuentaId() == null || event.cuentaId() <= 0) {
            log.warn("MovimientoRegistradoEvent invalido para SSE eventId={}", event == null ? null : event.eventId());
            return;
        }

        MovimientoEventDTO dto = new MovimientoEventDTO(
                event.cuentaId(),
                event.movimientoId(),
                event.tipoMovimiento(),
                event.importe(),
                event.saldoResultante(),
                "Movimiento registrado",
                fechaEvento(event),
                null
        );

        log.info(
                "MovimientoRegistradoEvent recibido para SSE cuentaId={} movimientoId={} correlationId={}",
                event.cuentaId(),
                event.movimientoId(),
                event.correlationId()
        );
        movimientoEventService.publicar(dto);
        log.info(
                "MovimientoRegistradoEvent enviado al bus SSE cuentaId={} movimientoId={}",
                event.cuentaId(),
                event.movimientoId()
        );
    }

    private LocalDateTime fechaEvento(MovimientoRegistradoEvent event) {
        if (event.occurredAt() == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault());
    }
}
