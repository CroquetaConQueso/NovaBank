package com.novabank.cuenta.service.strategy;

import com.novabank.cuenta.model.CuentaNumeroSecuencia;
import com.novabank.cuenta.repository.CuentaNumeroSecuenciaRepository;
import com.novabank.cuenta.repository.CuentaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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
     * Mantiene el Strategy Pattern usando una fila de secuencia bloqueada en
     * base de datos para reducir el riesgo de numeros duplicados.
     */
    @Override
    @Transactional
    public Mono<String> generarNumeroCuenta() {
        return secuenciaRepository.findByIdForUpdate(SEQUENCE_ID)
                .switchIfEmpty(secuenciaRepository.save(new CuentaNumeroSecuencia(SEQUENCE_ID, INITIAL_VALUE)))
                .flatMap(this::generarDesdeSecuencia);
    }

    private Mono<String> generarDesdeSecuencia(CuentaNumeroSecuencia secuencia) {
        return buscarNumeroDisponible(secuencia.getNextValue())
                .flatMap(numeroDisponible -> {
                    secuencia.setNextValue(extraerSecuencia(numeroDisponible) + 1);
                    return secuenciaRepository.save(secuencia).thenReturn(numeroDisponible);
                });
    }

    private Mono<String> buscarNumeroDisponible(long siguiente) {
        String numeroCuenta = formatear(siguiente);

        return cuentaRepository.existsByNumeroCuenta(numeroCuenta)
                .flatMap(existe -> existe
                        ? buscarNumeroDisponible(siguiente + 1)
                        : Mono.just(numeroCuenta));
    }

    private long extraerSecuencia(String numeroCuenta) {
        return Long.parseLong(numeroCuenta.substring(PREFIX.length()));
    }

    private String formatear(long secuencia) {
        return PREFIX + String.format("%012d", secuencia);
    }
}
