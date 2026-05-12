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
            .directBestEffort();
    private final AtomicLong eventosDescartados = new AtomicLong();

    /**
     * Bus en memoria para SSE. Prioriza la estabilidad del emisor: un
     * consumidor lento puede perder eventos y no hay replay tras reinicios.
     */
    public void publicar(MovimientoEventDTO evento) {
        Sinks.EmitResult resultado = sink.tryEmitNext(evento);
        if (resultado.isFailure()) {
            long total = eventosDescartados.incrementAndGet();
            log.warn("evento SSE descartado resultado={} cuentaId={} totalDescartados={}",
                    resultado,
                    evento != null ? evento.cuentaId() : null,
                    total);
        }
    }

    public Flux<MovimientoEventDTO> streamDeCuenta(Long cuentaId) {
        return Flux.defer(() -> {
            if (cuentaId == null || cuentaId <= 0) {
                return Flux.error(new IllegalArgumentException("El id de la cuenta debe ser positivo"));
            }

            return sink.asFlux()
                    .filter(evento -> cuentaId.equals(evento.cuentaId()))
                    .onBackpressureDrop(evento -> {
                        long total = eventosDescartados.incrementAndGet();
                        log.warn("evento SSE descartado por backpressure cuentaId={} totalDescartados={}",
                                evento.cuentaId(),
                                total);
                    });
        });
    }

    long eventosDescartados() {
        return eventosDescartados.get();
    }
}
