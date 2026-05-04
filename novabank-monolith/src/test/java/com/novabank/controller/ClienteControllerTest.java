package com.novabank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.dto.ClienteRequestDTO;
import com.novabank.dto.ClienteResponseDTO;
import com.novabank.exception.DuplicateResourceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteService clienteService;

    @Test
    void crearClienteDevuelveCreatedYBody() throws Exception {
        ClienteResponseDTO response = new ClienteResponseDTO(
                1L,
                "Ana",
                "Garcia",
                "12345678Z",
                "ana@example.com",
                "600111222",
                LocalDateTime.now(),
                0
        );
        when(clienteService.crearCliente(any(ClienteRequestDTO.class))).thenReturn(response);

        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678Z",
                "ana@example.com",
                "600111222"
        );

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dni").value("12345678Z"));
    }

    @Test
    void crearClienteConRequestInvalidoDevuelveFieldErrors() throws Exception {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "",
                "Garcia",
                "dni",
                "email-invalido",
                "123"
        );

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.nombre").exists())
                .andExpect(jsonPath("$.fieldErrors.dni").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.telefono").exists());
    }

    @Test
    void crearClienteConDniInvalidoDevuelve400() throws Exception {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678A",
                "ana@example.com",
                "600111222"
        );

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.dni").exists());
    }

    @Test
    void obtenerClienteNoEncontradoDevuelve404() throws Exception {
        when(clienteService.obtenerCliente(99L))
                .thenThrow(new ResourceNotFoundException("No existe ningun cliente con id 99"));

        mockMvc.perform(get("/api/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No existe ningun cliente con id 99"));
    }

    @Test
    void crearClienteDuplicadoDevuelve409() throws Exception {
        when(clienteService.crearCliente(any(ClienteRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Ya existe un cliente con el DNI 12345678Z"));

        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ana",
                "Garcia",
                "12345678Z",
                "ana@example.com",
                "600111222"
        );

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ya existe un cliente con el DNI 12345678Z"));
    }

    @Test
    void listarClientesDevuelveArray() throws Exception {
        when(clienteService.listarClientes()).thenReturn(List.of(
                new ClienteResponseDTO(
                        1L,
                        "Ana",
                        "Garcia",
                        "12345678Z",
                        "ana@example.com",
                        "600111222",
                        LocalDateTime.now(),
                        0
                )
        ));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("ana@example.com"));
    }

    @Test
    void obtenerClientePorDniDevuelveCliente() throws Exception {
        when(clienteService.obtenerClientePorDni("12345678Z")).thenReturn(
                new ClienteResponseDTO(
                        1L,
                        "Ana",
                        "Garcia",
                        "12345678Z",
                        "ana@example.com",
                        "600111222",
                        LocalDateTime.now(),
                        0
                )
        );

        mockMvc.perform(get("/api/clientes").param("dni", "12345678Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dni").value("12345678Z"));
    }
}
