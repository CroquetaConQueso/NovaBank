package com.novabank.cuenta.event;

import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.service.CuentaService;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Service
public class CuentaOperacionEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(CuentaOperacionEventProcessor.class);

    private final CuentaService cuentaService;
    private final OperacionResultadoEventPublisher publisher;

    public CuentaOperacionEventProcessor(
            CuentaService cuentaService,
            OperacionResultadoEventPublisher publisher
    ) {
        this.cuentaService = cuentaService;
        this.publisher = publisher;
    }

    public Mono<Void> procesar(Message<OperacionSolicitadaEvent> message) {
        OperacionSolicitadaEvent event = message.getPayload();

        log.info(
                "OperacionSolicitadaEvent recibido operationId={} tipoOperacion={} cuentaOrigenId={} "
                        + "cuentaDestinoId={} correlationId={}",
                event.operationId(),
                event.tipoOperacion(),
                event.cuentaOrigenId(),
                event.cuentaDestinoId(),
                event.correlationId()
        );

        return aplicarOperacion(event)
                .thenReturn(true)
                .onErrorResume(error -> publicarFalloNegocio(event, error).thenReturn(false))
                .flatMap(aplicada -> aplicada ? publicarCompletada(event) : Mono.empty())
                .contextWrite(context -> {
                    if (event.operationId() != null) {
                        context = context.put(CorrelationIdSupport.OPERATION_ID_CONTEXT_KEY, event.operationId().toString());
                    }
                    if (event.correlationId() != null) {
                        context = context.put(CorrelationIdSupport.CONTEXT_KEY, event.correlationId().toString());
                    }
                    return context;
                });
    }

    private Mono<Void> aplicarOperacion(OperacionSolicitadaEvent event) {
        return Mono.defer(() -> switch (tipoNormalizado(event.tipoOperacion())) {
            case "DEPOSITO" -> cuentaService.depositar(
                    cuentaDestinoObligatoria(event),
                    new CuentaOperacionRequestDTO(event.importe())
            ).then();
            case "RETIRO", "RETIRADA" -> cuentaService.retirar(
                    cuentaOrigenObligatoria(event),
                    new CuentaOperacionRequestDTO(event.importe())
            ).then();
            case "TRANSFERENCIA" -> cuentaService.transferir(new TransferenciaInternaRequestDTO(
                    cuentaOrigenObligatoria(event),
                    cuentaDestinoTransferenciaObligatoria(event),
                    event.importe()
            )).then();
            default -> Mono.error(new IllegalArgumentException(
                    "Tipo de operacion no soportado: " + event.tipoOperacion()
            ));
        });
    }

    private Mono<Void> publicarCompletada(OperacionSolicitadaEvent event) {
        return Mono.defer(() -> publisher.publicarCompletada(event))
                .doOnSuccess(ignored -> log.info(
                        "Operacion aplicada y resultado COMPLETADA publicado operationId={} tipoOperacion={} "
                                + "cuentaOrigenId={} cuentaDestinoId={}",
                        event.operationId(),
                        event.tipoOperacion(),
                        event.cuentaOrigenId(),
                        event.cuentaDestinoId()
                ))
                .doOnError(error -> log.error(
                        "Operacion aplicada pero no se pudo publicar COMPLETADA operationId={} tipoOperacion={} "
                                + "cuentaOrigenId={} cuentaDestinoId={} motivo={}",
                        event.operationId(),
                        event.tipoOperacion(),
                        event.cuentaOrigenId(),
                        event.cuentaDestinoId(),
                        motivo(error),
                        error
                ));
    }

    private Mono<Void> publicarFalloNegocio(OperacionSolicitadaEvent event, Throwable error) {
        String codigoError = resolverCodigoError(error);
        String motivo = motivo(error);

        log.warn(
                "Operacion rechazada operationId={} tipoOperacion={} cuentaOrigenId={} cuentaDestinoId={} "
                        + "codigoError={} motivo={}",
                event.operationId(),
                event.tipoOperacion(),
                event.cuentaOrigenId(),
                event.cuentaDestinoId(),
                codigoError,
                motivo
        );

        return publisher.publicarFallida(event, codigoError, motivo);
    }

    private String motivo(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private String tipoNormalizado(String tipoOperacion) {
        return tipoOperacion == null ? "" : tipoOperacion.trim().toUpperCase(Locale.ROOT);
    }

    private Long cuentaDestinoObligatoria(OperacionSolicitadaEvent event) {
        if (event.cuentaDestinoId() == null) {
            throw new IllegalArgumentException("La cuenta destino es obligatoria para depositos");
        }
        return event.cuentaDestinoId();
    }

    private Long cuentaOrigenObligatoria(OperacionSolicitadaEvent event) {
        if (event.cuentaOrigenId() == null) {
            throw new IllegalArgumentException("La cuenta origen es obligatoria para retiradas");
        }
        return event.cuentaOrigenId();
    }

    private Long cuentaDestinoTransferenciaObligatoria(OperacionSolicitadaEvent event) {
        if (event.cuentaDestinoId() == null) {
            throw new IllegalArgumentException("La cuenta destino es obligatoria para transferencias");
        }
        return event.cuentaDestinoId();
    }

    private String resolverCodigoError(Throwable error) {
        if (error instanceof InsufficientBalanceException) {
            return "SALDO_INSUFICIENTE";
        }
        if (error instanceof ResourceNotFoundException) {
            return "CUENTA_NO_ENCONTRADA";
        }
        if (error instanceof IllegalArgumentException) {
            return "SOLICITUD_INVALIDA";
        }
        return "OPERACION_FALLIDA";
    }
}
