package com.novabank.service.strategy;

import com.novabank.repository.CuentaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneradorNumeroCuentaSecuencialTest {

    private final CuentaRepository cuentaRepository = mock(CuentaRepository.class);
    private final GeneradorNumeroCuentaSecuencial generador = new GeneradorNumeroCuentaSecuencial(cuentaRepository);

    @Test
    void generaNumeroConPrefijoFuncionalYDoceDigitosSecuenciales() {
        when(cuentaRepository.count()).thenReturn(0L);
        when(cuentaRepository.existsByNumeroCuenta("ES91210000000000000001")).thenReturn(false);

        String numeroCuenta = generador.generarNumeroCuenta();

        assertThat(numeroCuenta).isEqualTo("ES91210000000000000001");
    }

    @Test
    void avanzaSecuenciaSiElNumeroYaExiste() {
        when(cuentaRepository.count()).thenReturn(0L);
        when(cuentaRepository.existsByNumeroCuenta("ES91210000000000000001")).thenReturn(true);
        when(cuentaRepository.existsByNumeroCuenta("ES91210000000000000002")).thenReturn(false);

        String numeroCuenta = generador.generarNumeroCuenta();

        assertThat(numeroCuenta).isEqualTo("ES91210000000000000002");
    }
}
