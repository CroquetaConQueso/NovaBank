package com.novabank.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.auth.dto.LoginRequestDTO;
import com.novabank.auth.dto.LoginResponseDTO;
import com.novabank.auth.dto.RegisterRequestDTO;
import com.novabank.auth.dto.RegisterResponseDTO;
import com.novabank.auth.dto.ValidateTokenResponseDTO;
import com.novabank.auth.exception.DuplicateUserException;
import com.novabank.auth.exception.GlobalExceptionHandler;
import com.novabank.auth.exception.InvalidCredentialsException;
import com.novabank.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void registerDevuelveCreated() throws Exception {
        when(authService.registrar(any(RegisterRequestDTO.class)))
                .thenReturn(new RegisterResponseDTO(1L, "ana", "USER", true, LocalDateTime.now()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequestDTO("ana", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    void registerDuplicadoDevuelve409() throws Exception {
        when(authService.registrar(any(RegisterRequestDTO.class)))
                .thenThrow(new DuplicateUserException("Ya existe un usuario con ese username"));

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Correlation-Id", "corr-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequestDTO("ana", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.correlationId").value("corr-auth"));
    }

    @Test
    void loginCorrectoDevuelveTokenBearer() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO("jwt-token", "Bearer", 86400000L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("ana", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void loginIncorrectoDevuelve401() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new InvalidCredentialsException("Credenciales invalidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("ana", "bad-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validateDevuelveResultado() throws Exception {
        when(authService.validarToken(eq("jwt-token")))
                .thenReturn(new ValidateTokenResponseDTO(true, "ana"));

        mockMvc.perform(get("/api/auth/validate").param("token", "jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    void loginRequestInvalidoDevuelveFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
