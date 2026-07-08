package com.novabank.operacion.application.port.in;

import java.util.UUID;

public record ConsultarEstadoOperacionQuery(UUID operationId) {
}
