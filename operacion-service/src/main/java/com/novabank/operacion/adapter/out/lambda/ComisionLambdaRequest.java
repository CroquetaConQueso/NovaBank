package com.novabank.operacion.adapter.out.lambda;

import java.math.BigDecimal;

public record ComisionLambdaRequest(
        BigDecimal importeEuros,
        String paisDestino,
        String tipoCliente
) {
}
