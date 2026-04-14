package com.novabank.service.strategy;

import com.novabank.persistence.repository.CuentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para GeneradorNumeroCuentaSecuencial.
 */
@ExtendWith(MockitoExtension.class)
class GeneradorNumeroCuentaSecuencialTest {

    @Mock
    private CuentaRepository repoCuenta;

    @Test
    void generarNumeroCuenta_siNoHayCuentas_debeGenerarPrimeraCuenta() {
        when(repoCuenta.obtenerUltimoIdCuenta()).thenReturn(0L);

        GeneradorNumeroCuentaSecuencial strategy =
                new GeneradorNumeroCuentaSecuencial(repoCuenta);

        String resultado = strategy.generarNumeroCuenta();

        assertEquals("ES91210000000000000001", resultado);
        verify(repoCuenta).obtenerUltimoIdCuenta();
    }

    @Test
    void generarNumeroCuenta_siYaHayCuentas_debeGenerarSiguienteNumero() {
        when(repoCuenta.obtenerUltimoIdCuenta()).thenReturn(44L);

        GeneradorNumeroCuentaSecuencial strategy =
                new GeneradorNumeroCuentaSecuencial(repoCuenta);

        String resultado = strategy.generarNumeroCuenta();

        assertEquals("ES91210000000000000045", resultado);
        verify(repoCuenta).obtenerUltimoIdCuenta();
    }

    @Test
    void generarNumeroCuenta_debeMantenerFormatoConCerosALaIzquierda() {
        when(repoCuenta.obtenerUltimoIdCuenta()).thenReturn(7L);

        GeneradorNumeroCuentaSecuencial strategy =
                new GeneradorNumeroCuentaSecuencial(repoCuenta);

        String resultado = strategy.generarNumeroCuenta();

        assertEquals("ES91210000000000000008", resultado);
        verify(repoCuenta).obtenerUltimoIdCuenta();
    }
}