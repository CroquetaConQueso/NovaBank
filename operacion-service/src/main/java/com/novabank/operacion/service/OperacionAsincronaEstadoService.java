package com.novabank.operacion.service;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import com.novabank.operacion.dto.OperacionEstadoResponseDTO;
import com.novabank.operacion.exception.OperacionAsincronaNotFoundException;
import com.novabank.operacion.model.OperacionAsincrona;
import com.novabank.operacion.repository.OperacionAsincronaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class OperacionAsincronaEstadoService {

    private static final Logger log = LoggerFactory.getLogger(OperacionAsincronaEstadoService.class);

    private final OperacionAsincronaRepository repository;

    public OperacionAsincronaEstadoService(OperacionAsincronaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Mono<OperacionAsincrona> crearSolicitada(OperacionSolicitadaEvent event, Long cuentaId) {
        OperacionAsincrona operacion = OperacionAsincrona.builder()
                .operationId(event.operationId())
                .correlationId(event.correlationId())
                .tipoOperacion(event.tipoOperacion())
                .cuentaId(cuentaId)
                .cuentaOrigenId(event.cuentaOrigenId())
                .cuentaDestinoId(event.cuentaDestinoId())
                .importe(event.importe())
                .moneda(event.moneda())
                .build();
        operacion.prepararParaCreacion();

        return repository.save(operacion)
                .doOnSuccess(guardada -> log.info(
                        "operacion asincrona registrada operationId={} estado={}",
                        guardada.getOperationId(),
                        guardada.getEstado()
                ));
    }

    @Transactional
    public Mono<Void> marcarCompletada(OperacionCompletadaEvent event) {
        return repository.findById(event.operationId())
                .flatMap(operacion -> {
                    if (operacion.estaCompletada()) {
                        log.info("OperacionCompletadaEvent duplicado no-op operationId={}", event.operationId());
                        return Mono.just(true);
                    }

                    operacion.marcarCompletada();
                    return repository.save(operacion)
                            .doOnSuccess(actualizada -> log.info(
                                    "operacion asincrona actualizada a COMPLETADA operationId={}",
                                    actualizada.getOperationId()
                            ))
                            .thenReturn(true);
                })
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn(
                        "OperacionCompletadaEvent sin operacion registrada operationId={}",
                        event.operationId()
                )).thenReturn(false))
                .then();
    }

    @Transactional
    public Mono<Void> marcarFallida(OperacionFallidaEvent event) {
        return repository.findById(event.operationId())
                .flatMap(operacion -> {
                    if (operacion.estaFallida()) {
                        log.info("OperacionFallidaEvent duplicado no-op operationId={}", event.operationId());
                        return Mono.just(true);
                    }

                    operacion.marcarFallida(event.motivo());
                    return repository.save(operacion)
                            .doOnSuccess(actualizada -> log.info(
                                    "operacion asincrona actualizada a FALLIDA operationId={} motivo={}",
                                    actualizada.getOperationId(),
                                    actualizada.getMotivoFallo()
                            ))
                            .thenReturn(true);
                })
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn(
                        "OperacionFallidaEvent sin operacion registrada operationId={}",
                        event.operationId()
                )).thenReturn(false))
                .then();
    }

    @Transactional
    public Mono<Void> marcarFallidaPorPublicacion(OperacionSolicitadaEvent event, Throwable error) {
        String motivo = "No se pudo publicar OperacionSolicitadaEvent: " + error.getMessage();

        return repository.findById(event.operationId())
                .flatMap(operacion -> {
                    operacion.marcarFallida(motivo);
                    return repository.save(operacion).then();
                });
    }

    @Transactional(readOnly = true)
    public Mono<OperacionEstadoResponseDTO> consultar(UUID operationId) {
        return repository.findById(operationId)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new OperacionAsincronaNotFoundException(
                        "No existe operacion asincrona con operationId " + operationId
                )));
    }

    private OperacionEstadoResponseDTO toResponse(OperacionAsincrona operacion) {
        return new OperacionEstadoResponseDTO(
                operacion.getOperationId(),
                operacion.getCorrelationId(),
                operacion.getTipoOperacion(),
                operacion.getEstado().name(),
                operacion.getCuentaId(),
                operacion.getCuentaOrigenId(),
                operacion.getCuentaDestinoId(),
                operacion.getImporte(),
                operacion.getMoneda(),
                operacion.getMotivoFallo(),
                operacion.getCreadaEn(),
                operacion.getActualizadaEn()
        );
    }
}
