package com.novabank.cuenta.application.usecase;

import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaCommand;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaResultado;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaUseCase;
import com.novabank.cuenta.application.port.out.AplicarOperacionCuentaPort;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Service
public class ProcesarOperacionSolicitadaService implements ProcesarOperacionSolicitadaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcesarOperacionSolicitadaService.class);

    private final AplicarOperacionCuentaPort aplicarOperacionCuentaPort;

    public ProcesarOperacionSolicitadaService(AplicarOperacionCuentaPort aplicarOperacionCuentaPort) {
        this.aplicarOperacionCuentaPort = aplicarOperacionCuentaPort;
    }

    @Override
    public Mono<ProcesarOperacionSolicitadaResultado> procesar(ProcesarOperacionSolicitadaCommand command) {
        log.info(
                "OperacionSolicitadaEvent recibido operationId={} tipoOperacion={} cuentaOrigenId={} "
                        + "cuentaDestinoId={} correlationId={}",
                command.operationId(),
                command.tipoOperacion(),
                command.cuentaOrigenId(),
                command.cuentaDestinoId(),
                command.correlationId()
        );

        return aplicarOperacion(command)
                .thenReturn(ProcesarOperacionSolicitadaResultado.completada(command))
                .doOnSuccess(resultado -> log.info(
                        "Operacion aplicada operationId={} tipoOperacion={} cuentaOrigenId={} cuentaDestinoId={}",
                        command.operationId(),
                        command.tipoOperacion(),
                        command.cuentaOrigenId(),
                        command.cuentaDestinoId()
                ))
                .onErrorResume(error -> resolverFalloNegocio(command, error))
                .contextWrite(context -> {
                    if (command.operationId() != null) {
                        context = context.put(
                                CorrelationIdSupport.OPERATION_ID_CONTEXT_KEY,
                                command.operationId().toString()
                        );
                    }
                    if (command.correlationId() != null) {
                        context = context.put(CorrelationIdSupport.CONTEXT_KEY, command.correlationId().toString());
                    }
                    return context;
                });
    }

    private Mono<Void> aplicarOperacion(ProcesarOperacionSolicitadaCommand command) {
        return Mono.defer(() -> switch (tipoNormalizado(command.tipoOperacion())) {
            case "DEPOSITO" -> aplicarOperacionCuentaPort.depositar(
                    cuentaDestinoObligatoria(command),
                    command.importe()
            );
            case "RETIRO", "RETIRADA" -> aplicarOperacionCuentaPort.retirar(
                    cuentaOrigenObligatoria(command),
                    command.importe()
            );
            case "TRANSFERENCIA" -> aplicarOperacionCuentaPort.transferir(
                    cuentaOrigenObligatoria(command),
                    cuentaDestinoTransferenciaObligatoria(command),
                    command.importe()
            );
            default -> Mono.error(new IllegalArgumentException(
                    "Tipo de operacion no soportado: " + command.tipoOperacion()
            ));
        });
    }

    private Mono<ProcesarOperacionSolicitadaResultado> resolverFalloNegocio(
            ProcesarOperacionSolicitadaCommand command,
            Throwable error
    ) {
        String codigoError = resolverCodigoError(error);
        String motivo = motivo(error);

        log.warn(
                "Operacion rechazada operationId={} tipoOperacion={} cuentaOrigenId={} cuentaDestinoId={} "
                        + "codigoError={} motivo={}",
                command.operationId(),
                command.tipoOperacion(),
                command.cuentaOrigenId(),
                command.cuentaDestinoId(),
                codigoError,
                motivo
        );

        return Mono.just(ProcesarOperacionSolicitadaResultado.fallida(command, codigoError, motivo));
    }

    private String motivo(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private String tipoNormalizado(String tipoOperacion) {
        return tipoOperacion == null ? "" : tipoOperacion.trim().toUpperCase(Locale.ROOT);
    }

    private Long cuentaDestinoObligatoria(ProcesarOperacionSolicitadaCommand command) {
        if (command.cuentaDestinoId() == null) {
            throw new IllegalArgumentException("La cuenta destino es obligatoria para depositos");
        }
        return command.cuentaDestinoId();
    }

    private Long cuentaOrigenObligatoria(ProcesarOperacionSolicitadaCommand command) {
        if (command.cuentaOrigenId() == null) {
            throw new IllegalArgumentException("La cuenta origen es obligatoria para retiradas");
        }
        return command.cuentaOrigenId();
    }

    private Long cuentaDestinoTransferenciaObligatoria(ProcesarOperacionSolicitadaCommand command) {
        if (command.cuentaDestinoId() == null) {
            throw new IllegalArgumentException("La cuenta destino es obligatoria para transferencias");
        }
        return command.cuentaDestinoId();
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
