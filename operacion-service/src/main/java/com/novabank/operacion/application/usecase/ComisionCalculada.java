package com.novabank.operacion.application.usecase;

import java.math.BigDecimal;

public record ComisionCalculada(
        BigDecimal comision,
        BigDecimal tasaAplicada,
        String paisDestino,
        String tipoCliente
) {

    public static ComisionCalculada noAplicable() {
        return new ComisionCalculada(null, null, null, null);
    }
}
