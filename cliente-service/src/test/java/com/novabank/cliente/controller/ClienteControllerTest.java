package com.novabank.cliente.controller;

import com.novabank.cliente.dto.ClienteRequestDTO;
import com.novabank.cliente.dto.ClienteResponseDTO;
import com.novabank.cliente.exception.DuplicateResourceException;
import com.novabank.cliente.exception.GlobalExceptionHandler;
import com.novabank.cliente.exception.ResourceNotFoundException;
import com.novabank.cliente.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(ClienteController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ClienteControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ClienteService clienteService;

    @Test
    void crearClienteDevuelveCreatedYBody() {
        when(clienteService.crearCliente(any(ClienteRequestDTO.class))).thenReturn(Mono.just(response()));

        webTestClient.post()
                .uri("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestValido())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.dni").isEqualTo("12345678Z");
    }

    @Test
    void actualizarClienteDevuelveOkYBody() {
        when(clienteService.actualizarCliente(any(Long.class), any(ClienteRequestDTO.class)))
                .thenReturn(Mono.just(response()));

        webTestClient.put()
                .uri("/api/clientes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestValido())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.email").isEqualTo("ana@example.com");
    }

    @Test
    void crearClienteConRequestInvalidoDevuelveFieldErrors() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "",
                "Garcia",
                "dni",
                "email-invalido",
                "123"
        );

        webTestClient.post()
                .uri("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.service").isEqualTo("cliente-service")
                .jsonPath("$.fieldErrors.nombre").exists()
                .jsonPath("$.fieldErrors.dni").exists()
                .jsonPath("$.fieldErrors.email").exists()
                .jsonPath("$.fieldErrors.telefono").exists();
    }

    @Test
    void crearClienteConBodyVacioDevuelve400() {
        webTestClient.post()
                .uri("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.fieldErrors.nombre").exists()
                .jsonPath("$.fieldErrors.apellidos").exists()
                .jsonPath("$.fieldErrors.dni").exists()
                .jsonPath("$.fieldErrors.email").exists()
                .jsonPath("$.fieldErrors.telefono").exists();
    }

    @Test
    void crearClienteConJsonMalformadoDevuelve400() {
        webTestClient.post()
                .uri("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST")
                .jsonPath("$.service").isEqualTo("cliente-service");
    }

    @Test
    void errorDevuelveJsonSimple() {
        when(clienteService.obtenerCliente(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ningun cliente con id 99")));

        webTestClient.get()
                .uri("/api/clientes/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND")
                .jsonPath("$.service").isEqualTo("cliente-service");
    }

    @Test
    void crearClienteDuplicadoDevuelve409() {
        when(clienteService.crearCliente(any(ClienteRequestDTO.class)))
                .thenReturn(Mono.error(new DuplicateResourceException("Ya existe un cliente con el DNI 12345678Z")));

        webTestClient.post()
                .uri("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestValido())
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT");
    }

    @Test
    void listarClientesDevuelveArray() {
        when(clienteService.listarClientes()).thenReturn(Flux.just(response()));

        webTestClient.get()
                .uri("/api/clientes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].email").isEqualTo("ana@example.com");
    }

    @Test
    void obtenerClientePorDniDevuelveCliente() {
        when(clienteService.obtenerClientePorDni("12345678Z")).thenReturn(Mono.just(response()));

        webTestClient.get()
                .uri("/api/clientes/dni/12345678Z")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.dni").isEqualTo("12345678Z");
    }

    @Test
    void obtenerClientePorDniInexistenteDevuelve404() {
        when(clienteService.obtenerClientePorDni("99999999R"))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ningun cliente con DNI 99999999R")));

        webTestClient.get()
                .uri("/api/clientes/dni/99999999R")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void actualizarClienteInexistenteDevuelve404() {
        when(clienteService.actualizarCliente(any(Long.class), any(ClienteRequestDTO.class)))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ningun cliente con id 99")));

        webTestClient.put()
                .uri("/api/clientes/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestValido())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void actualizarClienteConEmailDuplicadoDevuelve409() {
        when(clienteService.actualizarCliente(any(Long.class), any(ClienteRequestDTO.class)))
                .thenReturn(Mono.error(new DuplicateResourceException("Ya existe un cliente con el email ana@example.com")));

        webTestClient.put()
                .uri("/api/clientes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestValido())
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT");
    }

    @Test
    void obtenerClienteConIdCeroDevuelve400() {
        when(clienteService.obtenerCliente(0L))
                .thenReturn(Mono.error(new IllegalArgumentException("El id del cliente debe ser positivo")));

        webTestClient.get()
                .uri("/api/clientes/0")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    private ClienteRequestDTO requestValido() {
        return new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678Z",
                "ana@example.com",
                "600111222"
        );
    }

    private ClienteResponseDTO response() {
        return new ClienteResponseDTO(
                1L,
                "Ana",
                "Garcia",
                "12345678Z",
                "ana@example.com",
                "600111222",
                LocalDateTime.now()
        );
    }
}
