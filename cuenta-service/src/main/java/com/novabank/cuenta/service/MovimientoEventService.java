package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class MovimientoEventService {

    private final Sinks.Many<MovimientoEventDTO> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

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
                    .filter(evento -> cuentaId.equals(evento.cuentaId()));
        });
    }
}
