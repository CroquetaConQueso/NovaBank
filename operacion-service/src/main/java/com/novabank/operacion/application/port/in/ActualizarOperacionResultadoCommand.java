package com.novabank.operacion.application.port.in;

import java.util.UUID;

public record ActualizarOperacionResultadoCommand(
        UUID operationId,
        Resultado resultado,
        String codigoError,
        String motivo
) {

    public enum Resultado {
        COMPLETADA,
        FALLIDA
    }
}
