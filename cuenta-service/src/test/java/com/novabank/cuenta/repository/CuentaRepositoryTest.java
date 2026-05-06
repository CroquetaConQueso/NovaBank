package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.CuentaNumeroSecuencia;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CuentaRepositoryTest {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private CuentaNumeroSecuenciaRepository cuentaNumeroSecuenciaRepository;

    @Test
    void guardaYBuscaCuentaPorNumeroYClienteId() {
        Cuenta cuenta = cuenta("ES12345678901234567890", 1L, "0.00");

        Cuenta guardada = cuentaRepository.save(cuenta);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(guardada.getVersion()).isNotNull();
        assertThat(cuentaRepository.findByNumeroCuenta("ES12345678901234567890")).contains(guardada);
        assertThat(cuentaRepository.findByClienteId(1L)).containsExactly(guardada);
    }

    @Test
    void existePorNumeroCuentaDetectaDuplicados() {
        cuentaRepository.save(cuenta("ES12345678901234567891", 1L, "0.00"));

        assertThat(cuentaRepository.existsByNumeroCuenta("ES12345678901234567891")).isTrue();
        assertThat(cuentaRepository.existsByNumeroCuenta("ES12345678901234567892")).isFalse();
    }

    @Test
    void secuenciaDeNumeroCuentaPuedeIncrementarNextValueConBloqueo() {
        cuentaNumeroSecuenciaRepository.save(new CuentaNumeroSecuencia(1L, 1L));

        CuentaNumeroSecuencia secuencia = cuentaNumeroSecuenciaRepository.findByIdForUpdate(1L).orElseThrow();
        secuencia.setNextValue(secuencia.getNextValue() + 1);
        cuentaNumeroSecuenciaRepository.saveAndFlush(secuencia);

        assertThat(cuentaNumeroSecuenciaRepository.findById(1L).orElseThrow().getNextValue()).isEqualTo(2L);
    }

    private Cuenta cuenta(String numeroCuenta, Long clienteId, String saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setClienteId(clienteId);
        cuenta.setSaldo(new BigDecimal(saldo));
        return cuenta;
    }
}
