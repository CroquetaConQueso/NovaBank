package com.novabank.operacion.streams;

import java.util.UUID;

public record RetiradaWindowAggregate(
        long numeroOperaciones,
        UUID correlationId,
        String tipoOperacion
) {

    public static RetiradaWindowAggregate empty() {
        return new RetiradaWindowAggregate(0, null, null);
    }

    public RetiradaWindowAggregate incrementar(UUID nuevaCorrelationId, String nuevoTipoOperacion) {
        return new RetiradaWindowAggregate(numeroOperaciones + 1, nuevaCorrelationId, nuevoTipoOperacion);
    }
}
