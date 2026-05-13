package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MovimientoEventService {

    private static final Logger log = LoggerFactory.getLogger(MovimientoEventService.class);

    private final Sinks.Many<MovimientoEventDTO> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();
    private final AtomicLong eventosDescartados = new AtomicLong();

    /**
     * Bus en memoria para SSE. Los eventos no sobreviven a reinicios del
     * servicio y solo se entregan a suscriptores activos.
     */
    public void publicar(MovimientoEventDTO evento) {
        sink.tryEmitNext(evento);
    }

    public Flux<MovimientoEventDTO> streamDeCuenta(Long cuentaId) {
        return Flux.defer(() -> {
            if (cuentaId == null || cuentaId <= 0) {
                return Flux.error(new IllegalArgumentException("El id de la cuenta debe ser positivo"));
            }

            return sink.asFlux()
                    .filter(evento -> cuentaId.equals(evento.cuentaId()))
                    .onBackpressureDrop(this::registrarDescarte);
        });
    }

    long eventosDescartados() {
        return eventosDescartados.get();
    }

    private void registrarDescarte(MovimientoEventDTO evento) {
        long total = eventosDescartados.incrementAndGet();
        log.warn(
                "evento SSE descartado por backpressure cuentaId={} operationId={} descartados={}",
                evento.cuentaId(),
                evento.operationId(),
                total
        );
    }
}
