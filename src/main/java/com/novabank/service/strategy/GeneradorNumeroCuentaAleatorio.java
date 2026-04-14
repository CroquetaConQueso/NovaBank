package com.novabank.service.strategy;

import com.novabank.exception.NovaBankException;
import com.novabank.persistence.repository.CuentaRepository;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Estrategia concreta de generación aleatoria de números de cuenta.
 */
public class GeneradorNumeroCuentaAleatorio implements GeneradorNumeroCuentaStrategy {

    private static final int MAX_INTENTOS = 100;

    private final CuentaRepository repoCuenta;

    public GeneradorNumeroCuentaAleatorio(CuentaRepository repoCuenta) {
        this.repoCuenta = repoCuenta;
    }

    @Override
    public String generarNumeroCuenta() {
        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            long sufijo = ThreadLocalRandom.current().nextLong(1_000_000_000_000L);
            String numeroCuenta = "ES91210000" + String.format("%012d", sufijo);

            if (repoCuenta.buscarNumeroCuenta(numeroCuenta).isEmpty()) {
                return numeroCuenta;
            }
        }

        throw new NovaBankException("No se pudo generar un número de cuenta único.");
    }
}