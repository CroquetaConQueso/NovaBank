package com.novabank.cliente.repository;

import com.novabank.cliente.model.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void guardaYBuscaClientePorDniEmailYTelefono() {
        Cliente cliente = cliente("12345678Z", "ana.garcia@example.com", "600111222");

        Cliente guardado = clienteRepository.save(cliente);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(clienteRepository.findByDni("12345678Z")).contains(guardado);
        assertThat(clienteRepository.findByEmail("ana.garcia@example.com")).contains(guardado);
        assertThat(clienteRepository.findByTelefono("600111222")).contains(guardado);
    }

    @Test
    void buscarDuplicadosCuandoCoincideDniDevuelveCliente() {
        Cliente guardado = clienteRepository.save(cliente("12345678Z", "ana.garcia@example.com", "600111222"));

        assertThat(clienteRepository.buscarDuplicados("12345678Z", "no@coincide.com", "699999999"))
                .contains(guardado);
    }

    @Test
    void buscarDuplicadosExcluyendoIdIgnoraElClienteActual() {
        Cliente guardado = clienteRepository.save(cliente("12345678Z", "ana.garcia@example.com", "600111222"));

        assertThat(clienteRepository.buscarDuplicadosExcluyendoId(
                guardado.getId(),
                "12345678Z",
                "ana.garcia@example.com",
                "600111222"
        )).isEmpty();
    }

    @Test
    void buscarDuplicadosExcluyendoIdDetectaOtrosClientes() {
        clienteRepository.save(cliente("12345678Z", "ana.garcia@example.com", "600111222"));
        Cliente otro = clienteRepository.save(cliente("99999999R", "otro@example.com", "699999999"));

        assertThat(clienteRepository.buscarDuplicadosExcluyendoId(
                otro.getId(),
                "12345678Z",
                "nuevo@example.com",
                "688888888"
        )).hasSize(1);
    }

    private Cliente cliente(String dni, String email, String telefono) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Ana");
        cliente.setApellidos("Garcia Lopez");
        cliente.setDni(dni);
        cliente.setEmail(email);
        cliente.setTelefono(telefono);
        return cliente;
    }
}
