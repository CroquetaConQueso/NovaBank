package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.Cuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
class CuentaRepositoryTest {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private CuentaNumeroSecuenciaRepository cuentaNumeroSecuenciaRepository;

    @BeforeEach
    void setUp() {
        cuentaRepository.deleteAll().block();
    }

    @Test
    void guardaYBuscaCuentaPorNumeroYClienteId() {
        StepVerifier.create(cuentaRepository.save(cuenta("ES12345678901234567890", 1L, "0.00"))
                        .flatMap(guardada -> cuentaRepository.findByNumeroCuenta("ES12345678901234567890")
                                .zipWith(cuentaRepository.findByClienteId(1L).collectList())
                                .map(tuple -> guardada)))
                .assertNext(guardada -> {
                    assertThat(guardada.getId()).isNotNull();
                    assertThat(guardada.getFechaCreacion()).isNotNull();
                    assertThat(guardada.getVersion()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void existePorNumeroCuentaDetectaDuplicados() {
        StepVerifier.create(cuentaRepository.save(cuenta("ES12345678901234567891", 1L, "0.00"))
                        .then(cuentaRepository.existsByNumeroCuenta("ES12345678901234567891"))
                        .zipWith(cuentaRepository.existsByNumeroCuenta("ES12345678901234567892")))
                .assertNext(resultado -> {
                    assertThat(resultado.getT1()).isTrue();
                    assertThat(resultado.getT2()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void secuenciaDeNumeroCuentaPuedeIncrementarNextValueConBloqueo() {
        StepVerifier.create(cuentaNumeroSecuenciaRepository.findByIdForUpdate(1L)
                        .flatMap(secuencia -> {
                            secuencia.setNextValue(secuencia.getNextValue() + 1);
                            return cuentaNumeroSecuenciaRepository.save(secuencia);
                        })
                        .then(cuentaNumeroSecuenciaRepository.findById(1L)))
                .assertNext(secuencia -> assertThat(secuencia.getNextValue()).isEqualTo(2L))
                .verifyComplete();
    }

    private Cuenta cuenta(String numeroCuenta, Long clienteId, String saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setClienteId(clienteId);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.prepararParaCreacion();
        return cuenta;
    }
}
