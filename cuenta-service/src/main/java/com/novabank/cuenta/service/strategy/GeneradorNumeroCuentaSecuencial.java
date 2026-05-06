package com.novabank.cuenta.service.strategy;

import com.novabank.cuenta.model.CuentaNumeroSecuencia;
import com.novabank.cuenta.repository.CuentaNumeroSecuenciaRepository;
import com.novabank.cuenta.repository.CuentaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GeneradorNumeroCuentaSecuencial implements GeneradorNumeroCuentaStrategy {

    private static final long SEQUENCE_ID = 1L;
    private static final long INITIAL_VALUE = 1L;
    private static final String PREFIX = "ES91210000";

    private final CuentaNumeroSecuenciaRepository secuenciaRepository;
    private final CuentaRepository cuentaRepository;

    public GeneradorNumeroCuentaSecuencial(
            CuentaNumeroSecuenciaRepository secuenciaRepository,
            CuentaRepository cuentaRepository
    ) {
        this.secuenciaRepository = secuenciaRepository;
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * Usa una fila de secuencia protegida en base de datos para evitar numeros
     * duplicados cuando se crean cuentas concurrentemente.
     */
    @Override
    @Transactional
    public String generarNumeroCuenta() {
        CuentaNumeroSecuencia secuencia = secuenciaRepository.findByIdForUpdate(SEQUENCE_ID)
                .orElseGet(() -> secuenciaRepository.saveAndFlush(
                        new CuentaNumeroSecuencia(SEQUENCE_ID, INITIAL_VALUE)
                ));

        long siguiente = secuencia.getNextValue();
        String numeroCuenta = formatear(siguiente);

        while (cuentaRepository.existsByNumeroCuenta(numeroCuenta)) {
            siguiente++;
            numeroCuenta = formatear(siguiente);
        }

        secuencia.setNextValue(siguiente + 1);
        return numeroCuenta;
    }

    private String formatear(long secuencia) {
        return PREFIX + String.format("%012d", secuencia);
    }
}
