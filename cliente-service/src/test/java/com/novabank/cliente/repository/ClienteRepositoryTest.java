package com.novabank.cliente.repository;

import com.novabank.cliente.model.Cliente;
import com.novabank.cliente.testsupport.PostgresTestContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
class ClienteRepositoryTest extends PostgresTestContainerSupport {

    @Autowired
    private ClienteRepository clienteRepository;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll().block();
    }

    @Test
    void guardaYBuscaClientePorDniEmailYTelefono() {
        StepVerifier.create(clienteRepository.save(cliente("12345678Z", "ana.garcia@example.com", "600111222"))
                        .flatMap(guardado -> clienteRepository.findByDni("12345678Z")
                                .zipWith(clienteRepository.findByEmail("ana.garcia@example.com"))
                                .zipWith(clienteRepository.findByTelefono("600111222"))
                                .map(tuple -> guardado)))
                .assertNext(guardado -> {
                    assertThat(guardado.getId()).isNotNull();
                    assertThat(guardado.getFechaCreacion()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void buscarDuplicadosCuandoCoincideDniDevuelveCliente() {
        StepVerifier.create(clienteRepository.save(cliente("12345678Z", "ana.garcia@example.com", "600111222"))
                        .flatMapMany(guardado -> clienteRepository.buscarDuplicados(
                                "12345678Z",
                                "no@coincide.com",
                                "699999999"
                        )))
                .assertNext(encontrado -> assertThat(encontrado.getDni()).isEqualTo("12345678Z"))
                .verifyComplete();
    }

    @Test
    void buscarDuplicadosExcluyendoIdIgnoraElClienteActual() {
        StepVerifier.create(clienteRepository.save(cliente("12345678Z", "ana.garcia@example.com", "600111222"))
                        .flatMapMany(guardado -> clienteRepository.buscarDuplicadosExcluyendoId(
                                guardado.getId(),
                                "12345678Z",
                                "ana.garcia@example.com",
                                "600111222"
                        )))
                .verifyComplete();
    }

    @Test
    void buscarDuplicadosExcluyendoIdDetectaOtrosClientes() {
        StepVerifier.create(clienteRepository.save(cliente("12345678Z", "ana.garcia@example.com", "600111222"))
                        .then(clienteRepository.save(cliente("99999999R", "otro@example.com", "699999999")))
                        .flatMapMany(otro -> clienteRepository.buscarDuplicadosExcluyendoId(
                                otro.getId(),
                                "12345678Z",
                                "nuevo@example.com",
                                "688888888"
                        )))
                .assertNext(encontrado -> assertThat(encontrado.getDni()).isEqualTo("12345678Z"))
                .verifyComplete();
    }

    private Cliente cliente(String dni, String email, String telefono) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Ana");
        cliente.setApellidos("Garcia Lopez");
        cliente.setDni(dni);
        cliente.setEmail(email);
        cliente.setTelefono(telefono);
        cliente.prepararParaCreacion();
        return cliente;
    }
}
