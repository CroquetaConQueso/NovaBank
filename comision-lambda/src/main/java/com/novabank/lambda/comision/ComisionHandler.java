package com.novabank.lambda.comision;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

public class ComisionHandler implements RequestHandler<ComisionRequest, ComisionResponse> {

    private static final String CLIENTE_EMPRESA = "EMPRESA";
    private static final String CLIENTE_PARTICULAR = "PARTICULAR";
    private static final BigDecimal TASA_DEFAULT = new BigDecimal("0.025");
    private static final BigDecimal DESCUENTO_EMPRESA = new BigDecimal("0.80");
    private static final Map<String, BigDecimal> TASAS_POR_PAIS = Map.of(
            "US", new BigDecimal("0.020"),
            "GB", new BigDecimal("0.018"),
            "MX", new BigDecimal("0.030"),
            "MA", new BigDecimal("0.035")
    );

    @Override
    public ComisionResponse handleRequest(ComisionRequest request, Context context) {
        validar(request);

        String paisDestino = normalizar(request.getPaisDestino());
        String tipoCliente = normalizarTipoCliente(request.getTipoCliente());
        BigDecimal tasaAplicada = calcularTasa(paisDestino, tipoCliente);
        BigDecimal comision = request.getImporteEuros()
                .multiply(tasaAplicada)
                .setScale(2, RoundingMode.HALF_UP);

        return new ComisionResponse(
                comision,
                tasaAplicada.setScale(3, RoundingMode.HALF_UP),
                paisDestino,
                tipoCliente
        );
    }

    private void validar(ComisionRequest request) {
        if (request == null) {
            throw new ComisionValidationException("La solicitud de comision es obligatoria");
        }
        if (request.getImporteEuros() == null) {
            throw new ComisionValidationException("importeEuros es obligatorio");
        }
        if (request.getImporteEuros().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ComisionValidationException("importeEuros debe ser mayor que cero");
        }
        if (esBlank(request.getPaisDestino())) {
            throw new ComisionValidationException("paisDestino es obligatorio");
        }
        if (esBlank(request.getTipoCliente())) {
            throw new ComisionValidationException("tipoCliente es obligatorio");
        }
    }

    private BigDecimal calcularTasa(String paisDestino, String tipoCliente) {
        BigDecimal tasaBase = TASAS_POR_PAIS.getOrDefault(paisDestino, TASA_DEFAULT);
        if (CLIENTE_EMPRESA.equals(tipoCliente)) {
            return tasaBase.multiply(DESCUENTO_EMPRESA);
        }
        return tasaBase;
    }

    private String normalizarTipoCliente(String tipoCliente) {
        String normalizado = normalizar(tipoCliente);
        if (CLIENTE_EMPRESA.equals(normalizado)) {
            return CLIENTE_EMPRESA;
        }
        return CLIENTE_PARTICULAR;
    }

    private String normalizar(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private boolean esBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
