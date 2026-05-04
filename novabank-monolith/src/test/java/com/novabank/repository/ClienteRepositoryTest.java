package com.novabank.repository;

import com.novabank.model.Cliente;
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
        Cliente cliente = Cliente.builder()
                .nombre("Ana")
                .apellidos("Garcia Lopez")
                .dni("12345678Z")
                .email("ana.garcia@example.com")
                .telefono("600111222")
                .build();

        Cliente guardado = clienteRepository.save(cliente);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(clienteRepository.findByDni("12345678Z")).contains(guardado);
        assertThat(clienteRepository.findByEmail("ana.garcia@example.com")).contains(guardado);
        assertThat(clienteRepository.findByTelefono("600111222")).contains(guardado);
    }

    @Test
    void buscarDuplicadosCuandoCoincideDniDevuelveCliente() {
        Cliente cliente = Cliente.builder()
                .nombre("Ana")
                .apellidos("Garcia Lopez")
                .dni("12345678Z")
                .email("ana.garcia@example.com")
                .telefono("600111222")
                .build();

        Cliente guardado = clienteRepository.save(cliente);

        assertThat(clienteRepository.buscarDuplicados("12345678Z", "no@coincide.com", "699999999"))
                .contains(guardado);
    }

    @Test
    void buscarDuplicadosCuandoCoincideEmailDevuelveCliente() {
        Cliente cliente = Cliente.builder()
                .nombre("Ana")
                .apellidos("Garcia Lopez")
                .dni("12345678Z")
                .email("ana.garcia@example.com")
                .telefono("600111222")
                .build();

        Cliente guardado = clienteRepository.save(cliente);

        assertThat(clienteRepository.buscarDuplicados("99999999R", "ana.garcia@example.com", "699999999"))
                .contains(guardado);
    }

    @Test
    void buscarDuplicadosCuandoCoincideTelefonoDevuelveCliente() {
        Cliente cliente = Cliente.builder()
                .nombre("Ana")
                .apellidos("Garcia Lopez")
                .dni("12345678Z")
                .email("ana.garcia@example.com")
                .telefono("600111222")
                .build();

        Cliente guardado = clienteRepository.save(cliente);

        assertThat(clienteRepository.buscarDuplicados("99999999R", "no@coincide.com", "600111222"))
                .contains(guardado);
    }

    @Test
    void buscarDuplicadosCuandoNoCoincideNadaDevuelveListaVacia() {
        Cliente cliente = Cliente.builder()
                .nombre("Ana")
                .apellidos("Garcia Lopez")
                .dni("12345678Z")
                .email("ana.garcia@example.com")
                .telefono("600111222")
                .build();

        clienteRepository.save(cliente);

        assertThat(clienteRepository.buscarDuplicados("99999999R", "no@coincide.com", "699999999"))
                .isEmpty();
    }
}
