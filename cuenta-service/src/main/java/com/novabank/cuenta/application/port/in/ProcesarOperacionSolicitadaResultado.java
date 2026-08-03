package com.novabank.cuenta.application.port.in;

import java.util.UUID;

public record ProcesarOperacionSolicitadaResultado(
        UUID operationId,
        String tipoOperacion,
        Estado estado,
        String codigoError,
        String motivo
) {

    public enum Estado {
        COMPLETADA,
        FALLIDA
    }

    public static ProcesarOperacionSolicitadaResultado completada(ProcesarOperacionSolicitadaCommand command) {
        return new ProcesarOperacionSolicitadaResultado(
                command.operationId(),
                command.tipoOperacion(),
                Estado.COMPLETADA,
                null,
                null
        );
    }

    public static ProcesarOperacionSolicitadaResultado fallida(
            ProcesarOperacionSolicitadaCommand command,
            String codigoError,
            String motivo
    ) {
        return new ProcesarOperacionSolicitadaResultado(
                command.operationId(),
                command.tipoOperacion(),
                Estado.FALLIDA,
                codigoError,
                motivo
        );
    }
}
