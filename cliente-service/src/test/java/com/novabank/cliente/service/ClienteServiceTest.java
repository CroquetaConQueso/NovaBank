package com.novabank.cliente.service;

import com.novabank.cliente.dto.ClienteRequestDTO;
import com.novabank.cliente.exception.DuplicateResourceException;
import com.novabank.cliente.exception.ResourceNotFoundException;
import com.novabank.cliente.exception.ValidationException;
import com.novabank.cliente.mapper.ClienteMapper;
import com.novabank.cliente.model.Cliente;
import com.novabank.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Spy
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void crearClienteNormalizaDatosYGuardaCliente() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                " Ana ",
                " Garcia ",
                "12345678z",
                "ANA@example.COM",
                "600111222"
        );

        when(clienteRepository.buscarDuplicados("12345678Z", "ana@example.com", "600111222"))
                .thenReturn(Flux.empty());

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(1L);
            cliente.setFechaCreacion(LocalDateTime.now());
            return Mono.just(cliente);
        });

        StepVerifier.create(clienteService.crearCliente(request))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(1L);
                    assertThat(response.email()).isEqualTo("ana@example.com");
                })
                .verifyComplete();

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());

        Cliente guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Ana");
        assertThat(guardado.getApellidos()).isEqualTo("Garcia");
        assertThat(guardado.getDni()).isEqualTo("12345678Z");
        assertThat(guardado.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void actualizarClienteNormalizaYExcluyeSuPropioIdAlValidarDuplicados() {
        Cliente existente = cliente("12345678Z", "ana@example.com", "600111222");
        existente.setId(1L);
        existente.setFechaCreacion(LocalDateTime.now());

        ClienteRequestDTO request = new ClienteRequestDTO(
                " Ana Maria ",
                " Garcia ",
                "12345678z",
                "ANA.MARIA@example.COM",
                "600111333"
        );

        when(clienteRepository.findById(1L)).thenReturn(Mono.just(existente));
        when(clienteRepository.buscarDuplicadosExcluyendoId(1L, "12345678Z", "ana.maria@example.com", "600111333"))
                .thenReturn(Flux.empty());
        when(clienteRepository.save(existente)).thenReturn(Mono.just(existente));

        StepVerifier.create(clienteService.actualizarCliente(1L, request))
                .assertNext(response -> {
                    assertThat(response.nombre()).isEqualTo("Ana Maria");
                    assertThat(response.email()).isEqualTo("ana.maria@example.com");
                })
                .verifyComplete();

        verify(clienteRepository).buscarDuplicadosExcluyendoId(1L, "12345678Z", "ana.maria@example.com", "600111333");
    }

    @Test
    void crearClienteLanzaErrorSiDniYaExiste() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678Z",
                "ana@example.com",
                "600111222"
        );

        Cliente duplicado = cliente("12345678Z", "otro@example.com", "699999999");

        when(clienteRepository.buscarDuplicados("12345678Z", "ana@example.com", "600111222"))
                .thenReturn(Flux.just(duplicado));

        StepVerifier.create(clienteService.crearCliente(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DuplicateResourceException.class);
                    assertThat(error).hasMessageContaining("DNI");
                })
                .verify();
    }

    @Test
    void obtenerClientePorDniCuandoExisteDevuelveResponse() {
        Cliente cliente = cliente("12345678Z", "ana@example.com", "600111222");
        cliente.setId(1L);
        cliente.setFechaCreacion(LocalDateTime.now());

        when(clienteRepository.findByDni("12345678Z")).thenReturn(Mono.just(cliente));

        StepVerifier.create(clienteService.obtenerClientePorDni("12345678z"))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(1L);
                    assertThat(response.dni()).isEqualTo("12345678Z");
                })
                .verifyComplete();
    }

    @Test
    void obtenerClientePorDniCuandoNoExisteLanzaResourceNotFound() {
        when(clienteRepository.findByDni("12345678Z")).thenReturn(Mono.empty());

        StepVerifier.create(clienteService.obtenerClientePorDni("12345678Z"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                    assertThat(error).hasMessageContaining("DNI");
                })
                .verify();
    }

    @Test
    void obtenerClientePorDniCuandoBlankLanzaValidationException() {
        StepVerifier.create(clienteService.obtenerClientePorDni("   "))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ValidationException.class);
                    assertThat(error).hasMessage("El DNI es obligatorio");
                })
                .verify();
    }

    private Cliente cliente(String dni, String email, String telefono) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Ana");
        cliente.setApellidos("Garcia");
        cliente.setDni(dni);
        cliente.setEmail(email);
        cliente.setTelefono(telefono);
        return cliente;
    }
}
