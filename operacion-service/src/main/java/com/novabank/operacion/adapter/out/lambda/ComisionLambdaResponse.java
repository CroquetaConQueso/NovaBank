package com.novabank.operacion.adapter.out.lambda;

import java.math.BigDecimal;

public record ComisionLambdaResponse(
        BigDecimal comision,
        BigDecimal tasaAplicada,
        String paisDestino,
        String tipoCliente
) {
}
