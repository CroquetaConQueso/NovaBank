package com.novabank.repository;

import com.novabank.model.Cliente;
import com.novabank.model.Cuenta;
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
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Test
    void guardaYBuscaCuentaPorNumeroYCliente() {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Luis")
                .apellidos("Perez Martin")
                .dni("87654321B")
                .email("luis.perez@example.com")
                .telefono("600333444")
                .build());

        Cuenta cuenta = cuentaRepository.save(Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldo(BigDecimal.ZERO)
                .build());

        assertThat(cuenta.getId()).isNotNull();
        assertThat(cuenta.getFechaCreacion()).isNotNull();
        assertThat(cuentaRepository.findByNumeroCuenta("ES12345678901234567890")).contains(cuenta);
        assertThat(cuentaRepository.findByClienteId(cliente.getId())).containsExactly(cuenta);
    }
}
