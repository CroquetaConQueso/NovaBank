package com.novabank.operacion.application.port.in;

import java.util.UUID;

public record ActualizarOperacionResultadoResult(
        UUID operationId,
        String estado,
        boolean actualizada
) {
}
