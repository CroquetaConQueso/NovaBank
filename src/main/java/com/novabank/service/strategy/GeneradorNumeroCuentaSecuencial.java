package com.novabank.service.strategy;

import com.novabank.repository.CuentaRepository;
import org.springframework.stereotype.Component;

@Component
public class GeneradorNumeroCuentaSecuencial implements GeneradorNumeroCuentaStrategy {

    private static final String PREFIX = "ES91210000";

    private final CuentaRepository cuentaRepository;

    public GeneradorNumeroCuentaSecuencial(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public String generarNumeroCuenta() {
        long siguiente = cuentaRepository.count() + 1;
        String numeroCuenta = formatear(siguiente);

        while (cuentaRepository.existsByNumeroCuenta(numeroCuenta)) {
            siguiente++;
            numeroCuenta = formatear(siguiente);
        }

        return numeroCuenta;
    }

    private String formatear(long secuencia) {
        return PREFIX + String.format("%012d", secuencia);
    }
}
