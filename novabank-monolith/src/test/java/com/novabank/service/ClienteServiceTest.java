package com.novabank.service;

import com.novabank.dto.ClienteRequestDTO;
import com.novabank.dto.ClienteResponseDTO;
import com.novabank.exception.DuplicateResourceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.mapper.ClienteMapper;
import com.novabank.model.Cliente;
import com.novabank.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                "12345678a",
                "ANA@example.COM",
                "600111222"
        );

        when(clienteRepository.buscarDuplicados("12345678A", "ana@example.com", "600111222"))
                .thenReturn(List.of());

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(1L);
            cliente.setFechaCreacion(LocalDateTime.now());
            return cliente;
        });

        ClienteResponseDTO response = clienteService.crearCliente(request);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());

        Cliente guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Ana");
        assertThat(guardado.getApellidos()).isEqualTo("Garcia");
        assertThat(guardado.getDni()).isEqualTo("12345678A");
        assertThat(guardado.getEmail()).isEqualTo("ana@example.com");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.numeroCuentas()).isZero();
    }

    @Test
    void crearClienteLanzaErrorSiDniYaExiste() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678A",
                "ana@example.com",
                "600111222"
        );

        Cliente duplicado = Cliente.builder()
                .dni("12345678A")
                .email("otro@example.com")
                .telefono("699999999")
                .build();

        when(clienteRepository.buscarDuplicados("12345678A", "ana@example.com", "600111222"))
                .thenReturn(List.of(duplicado));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("DNI");
    }

    @Test
    void crearClienteLanzaErrorSiEmailYaExiste() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678A",
                "ana@example.com",
                "600111222"
        );

        Cliente duplicado = Cliente.builder()
                .dni("99999999R")
                .email("ana@example.com")
                .telefono("699999999")
                .build();

        when(clienteRepository.buscarDuplicados("12345678A", "ana@example.com", "600111222"))
                .thenReturn(List.of(duplicado));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
    }

    @Test
    void crearClienteLanzaErrorSiTelefonoYaExiste() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678A",
                "ana@example.com",
                "600111222"
        );

        Cliente duplicado = Cliente.builder()
                .dni("99999999R")
                .email("otro@example.com")
                .telefono("600111222")
                .build();

        when(clienteRepository.buscarDuplicados("12345678A", "ana@example.com", "600111222"))
                .thenReturn(List.of(duplicado));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("telefono");
    }

    @Test
    void crearClienteLanzaErrorSiRequestEsNulo() {
        assertThatThrownBy(() -> clienteService.crearCliente(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Los datos del cliente son obligatorios");
    }

    @Test
    void obtenerClientePorDniCuandoExisteDevuelveResponse() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Ana")
                .apellidos("Garcia")
                .dni("12345678A")
                .email("ana@example.com")
                .telefono("600111222")
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(clienteRepository.findByDni("12345678A")).thenReturn(Optional.of(cliente));

        ClienteResponseDTO response = clienteService.obtenerClientePorDni("12345678A");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.dni()).isEqualTo("12345678A");
    }

    @Test
    void obtenerClientePorDniCuandoNoExisteLanzaResourceNotFound() {
        when(clienteRepository.findByDni("12345678A")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerClientePorDni("12345678A"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DNI");
    }

    @Test
    void obtenerClientePorDniCuandoBlankLanzaValidationException() {
        assertThatThrownBy(() -> clienteService.obtenerClientePorDni("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El DNI es obligatorio");
    }

    @Test
    void obtenerClientePorDniNormalizaAntesDeBuscar() {
        when(clienteRepository.findByDni("12345678A")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerClientePorDni(" 12345678a "))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(clienteRepository).findByDni("12345678A");
    }
}
