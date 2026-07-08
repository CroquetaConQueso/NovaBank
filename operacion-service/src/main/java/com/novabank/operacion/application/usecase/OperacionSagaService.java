package com.novabank.operacion.application.usecase;

import com.novabank.operacion.application.port.in.ActualizarEstadoOperacionResultadoUseCase;
import com.novabank.operacion.application.port.in.ActualizarOperacionResultadoCommand;
import com.novabank.operacion.application.port.in.ActualizarOperacionResultadoResult;
import com.novabank.operacion.application.port.in.ConsultarEstadoOperacionQuery;
import com.novabank.operacion.application.port.in.ConsultarEstadoOperacionUseCase;
import com.novabank.operacion.application.port.in.EstadoOperacionAsincronaResult;
import com.novabank.operacion.application.port.in.OperacionAceptadaResult;
import com.novabank.operacion.application.port.in.SolicitarDepositoCommand;
import com.novabank.operacion.application.port.in.SolicitarDepositoUseCase;
import com.novabank.operacion.application.port.in.SolicitarRetiradaCommand;
import com.novabank.operacion.application.port.in.SolicitarRetiradaUseCase;
import com.novabank.operacion.application.port.in.SolicitarTransferenciaCommand;
import com.novabank.operacion.application.port.in.SolicitarTransferenciaUseCase;
import com.novabank.operacion.application.port.in.TransferenciaAceptadaResult;
import com.novabank.operacion.application.port.out.OperacionAsincronaRepositoryPort;
import com.novabank.operacion.application.port.out.OperacionSolicitadaPublisherPort;
import com.novabank.operacion.domain.model.EstadoOperacionAsincrona;
import com.novabank.operacion.domain.model.OperacionAsincrona;
import com.novabank.operacion.domain.model.OperacionSolicitada;
import com.novabank.operacion.exception.OperacionAsincronaNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OperacionSagaService implements
        SolicitarDepositoUseCase,
        SolicitarRetiradaUseCase,
        SolicitarTransferenciaUseCase,
        ConsultarEstadoOperacionUseCase,
        ActualizarEstadoOperacionResultadoUseCase {

    private static final Logger log = LoggerFactory.getLogger(OperacionSagaService.class);
    private static final String MONEDA_LOCAL = "EUR";

    private final OperacionAsincronaRepositoryPort repositoryPort;
    private final OperacionSolicitadaPublisherPort publisherPort;

    public OperacionSagaService(
            OperacionAsincronaRepositoryPort repositoryPort,
            OperacionSolicitadaPublisherPort publisherPort
    ) {
        this.repositoryPort = repositoryPort;
        this.publisherPort = publisherPort;
    }

    @Override
    @Transactional
    public Mono<OperacionAceptadaResult> solicitarDeposito(SolicitarDepositoCommand command) {
        return solicitarOperacionSimple(
                "DEPOSITO",
                command.cuentaId(),
                null,
                command.cuentaId(),
                command.cuentaId(),
                command.cantidad(),
                command.correlationId(),
                command.idempotencyKey()
        );
    }

    @Override
    @Transactional
    public Mono<OperacionAceptadaResult> solicitarRetirada(SolicitarRetiradaCommand command) {
        return solicitarOperacionSimple(
                "RETIRADA",
                command.cuentaId(),
                command.cuentaId(),
                null,
                command.cuentaId(),
                command.cantidad(),
                command.correlationId(),
                command.idempotencyKey()
        );
    }

    @Override
    @Transactional
    public Mono<TransferenciaAceptadaResult> solicitarTransferencia(SolicitarTransferenciaCommand command) {
        return Mono.defer(() -> {
            OperacionSolicitada solicitud = nuevaSolicitud(
                    "TRANSFERENCIA",
                    command.cuentaOrigenId(),
                    command.cuentaOrigenId(),
                    command.cuentaDestinoId(),
                    command.cantidad(),
                    command.correlationId(),
                    command.cuentaOrigenId()
            );

            log.info(
                    "transferencia asincrona recibida cuentaOrigenId={} cuentaDestinoId={} importe={} operationId={} idempotencyKey={}",
                    command.cuentaOrigenId(),
                    command.cuentaDestinoId(),
                    command.cantidad(),
                    solicitud.operationId(),
                    idempotencyState(command.idempotencyKey())
            );

            return registrarYPublicar(solicitud)
                    .thenReturn(new TransferenciaAceptadaResult(
                            solicitud.operationId(),
                            EstadoOperacionAsincrona.SOLICITADA.name(),
                            "TRANSFERENCIA solicitada para procesamiento asincrono",
                            "TRANSFERENCIA",
                            command.cuentaOrigenId(),
                            command.cuentaDestinoId(),
                            command.cantidad(),
                            MONEDA_LOCAL
                    ));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<EstadoOperacionAsincronaResult> consultar(ConsultarEstadoOperacionQuery query) {
        return repositoryPort.findByOperationId(query.operationId())
                .map(this::toEstadoResult)
                .switchIfEmpty(Mono.error(new OperacionAsincronaNotFoundException(
                        "No existe operacion asincrona con operationId " + query.operationId()
                )));
    }

    @Override
    @Transactional
    public Mono<ActualizarOperacionResultadoResult> actualizar(ActualizarOperacionResultadoCommand command) {
        return repositoryPort.findByOperationId(command.operationId())
                .flatMap(operacion -> actualizarExistente(operacion, command))
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn(
                        "resultado de operacion sin operacion registrada operationId={} resultado={}",
                        command.operationId(),
                        command.resultado()
                )).thenReturn(new ActualizarOperacionResultadoResult(
                        command.operationId(),
                        command.resultado().name(),
                        false
                )));
    }

    private Mono<OperacionAceptadaResult> solicitarOperacionSimple(
            String tipoOperacion,
            Long cuentaId,
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            Long kafkaKey,
            java.math.BigDecimal importe,
            UUID correlationId,
            String idempotencyKey
    ) {
        return Mono.defer(() -> {
            OperacionSolicitada solicitud = nuevaSolicitud(
                    tipoOperacion,
                    cuentaId,
                    cuentaOrigenId,
                    cuentaDestinoId,
                    importe,
                    correlationId,
                    kafkaKey
            );

            log.info(
                    "operacion asincrona recibida tipoOperacion={} cuentaId={} importe={} operationId={} idempotencyKey={}",
                    tipoOperacion,
                    cuentaId,
                    importe,
                    solicitud.operationId(),
                    idempotencyState(idempotencyKey)
            );

            return registrarYPublicar(solicitud)
                    .thenReturn(new OperacionAceptadaResult(
                            solicitud.operationId(),
                            EstadoOperacionAsincrona.SOLICITADA.name(),
                            tipoOperacion + " solicitada para procesamiento asincrono",
                            tipoOperacion,
                            cuentaId,
                            importe
                    ));
        });
    }

    private Mono<Void> registrarYPublicar(OperacionSolicitada solicitud) {
        LocalDateTime ahora = LocalDateTime.now();
        OperacionAsincrona operacion = new OperacionAsincrona(
                solicitud.operationId(),
                solicitud.correlationId(),
                solicitud.tipoOperacion(),
                solicitud.cuentaId(),
                solicitud.cuentaOrigenId(),
                solicitud.cuentaDestinoId(),
                solicitud.importe(),
                solicitud.moneda(),
                EstadoOperacionAsincrona.SOLICITADA,
                null,
                ahora,
                ahora
        );

        return repositoryPort.save(operacion)
                .doOnSuccess(guardada -> log.info(
                        "operacion asincrona registrada operationId={} estado={}",
                        guardada.operationId(),
                        guardada.estado()
                ))
                .then(publisherPort.publicar(solicitud))
                .onErrorResume(error -> marcarFallidaPorPublicacion(solicitud, error).then(Mono.error(error)));
    }

    private Mono<Void> marcarFallidaPorPublicacion(OperacionSolicitada solicitud, Throwable error) {
        String motivo = "No se pudo publicar OperacionSolicitadaEvent: " + error.getMessage();
        return repositoryPort.findByOperationId(solicitud.operationId())
                .flatMap(operacion -> repositoryPort.save(operacion.marcarFallida(motivo, LocalDateTime.now())))
                .then();
    }

    private Mono<ActualizarOperacionResultadoResult> actualizarExistente(
            OperacionAsincrona operacion,
            ActualizarOperacionResultadoCommand command
    ) {
        if (command.resultado() == ActualizarOperacionResultadoCommand.Resultado.COMPLETADA) {
            if (operacion.estaCompletada()) {
                log.info("OperacionCompletadaEvent duplicado no-op operationId={}", command.operationId());
                return Mono.just(new ActualizarOperacionResultadoResult(command.operationId(), "COMPLETADA", false));
            }

            return repositoryPort.save(operacion.marcarCompletada(LocalDateTime.now()))
                    .doOnSuccess(actualizada -> log.info(
                            "operacion asincrona actualizada a COMPLETADA operationId={}",
                            actualizada.operationId()
                    ))
                    .thenReturn(new ActualizarOperacionResultadoResult(command.operationId(), "COMPLETADA", true));
        }

        if (operacion.estaFallida()) {
            log.info("OperacionFallidaEvent duplicado no-op operationId={}", command.operationId());
            return Mono.just(new ActualizarOperacionResultadoResult(command.operationId(), "FALLIDA", false));
        }

        return repositoryPort.save(operacion.marcarFallida(command.motivo(), LocalDateTime.now()))
                .doOnSuccess(actualizada -> log.info(
                        "operacion asincrona actualizada a FALLIDA operationId={} motivo={}",
                        actualizada.operationId(),
                        actualizada.motivoFallo()
                ))
                .thenReturn(new ActualizarOperacionResultadoResult(command.operationId(), "FALLIDA", true));
    }

    private OperacionSolicitada nuevaSolicitud(
            String tipoOperacion,
            Long cuentaId,
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            java.math.BigDecimal importe,
            UUID correlationId,
            Long kafkaKey
    ) {
        return new OperacionSolicitada(
                UUID.randomUUID(),
                correlationId == null ? UUID.randomUUID() : correlationId,
                Instant.now(),
                UUID.randomUUID(),
                tipoOperacion,
                cuentaOrigenId,
                cuentaDestinoId,
                cuentaId,
                importe,
                MONEDA_LOCAL,
                kafkaKey
        );
    }

    private EstadoOperacionAsincronaResult toEstadoResult(OperacionAsincrona operacion) {
        return new EstadoOperacionAsincronaResult(
                operacion.operationId(),
                operacion.correlationId(),
                operacion.tipoOperacion(),
                operacion.estado().name(),
                operacion.cuentaId(),
                operacion.cuentaOrigenId(),
                operacion.cuentaDestinoId(),
                operacion.importe(),
                operacion.moneda(),
                operacion.motivoFallo(),
                operacion.creadaEn(),
                operacion.actualizadaEn()
        );
    }

    private String idempotencyState(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank() ? "no-informada" : "informada";
    }
}
