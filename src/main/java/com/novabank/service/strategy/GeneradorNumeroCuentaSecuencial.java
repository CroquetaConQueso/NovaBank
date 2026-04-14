package com.novabank.service.strategy;

import com.novabank.persistence.repository.CuentaRepository;

/**
 * Estrategia concreta de generación secuencial de números de cuenta.
 *
 * Toma el último id de cuenta persistido y genera el siguiente número
 * respetando el formato definido por la aplicación.
 */
public class GeneradorNumeroCuentaSecuencial implements GeneradorNumeroCuentaStrategy {

    private static final String PREFIJO = "ES91210000";
    private static final int LONGITUD_SUFJO = 12;

    private final CuentaRepository repoCuenta;

    public GeneradorNumeroCuentaSecuencial(CuentaRepository repoCuenta) {
        this.repoCuenta = repoCuenta;
    }

    @Override
    public String generarNumeroCuenta() {
        long siguienteId = repoCuenta.obtenerUltimoIdCuenta() + 1;
        return PREFIJO + String.format("%0" + LONGITUD_SUFJO + "d", siguienteId);
    }
}