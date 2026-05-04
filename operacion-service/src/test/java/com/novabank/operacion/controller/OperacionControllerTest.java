package com.novabank.operacion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.GlobalExceptionHandler;
import com.novabank.operacion.exception.IdempotencyConflictException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.model.EstadoOperacion;
import com.novabank.operacion.model.TipoOperacion;
import com.novabank.operacion.service.OperacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperacionController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class OperacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OperacionService operacionService;

    @Test
    void depositoDevuelveOperacionCompletada() throws Exception {
        when(operacionService.depositar(any(OperacionRequestDTO.class), eq("dep-1"), eq("corr-1")))
                .thenReturn(response(TipoOperacion.DEPOSITO, "dep-1"));

        mockMvc.perform(post("/api/operaciones/deposito")
                        .header("Idempotency-Key", "dep-1")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.tipoOperacion").value("DEPOSITO"));
    }

    @Test
    void retiroSinIdempotencyKeyDevuelve400() throws Exception {
        when(operacionService.retirar(any(OperacionRequestDTO.class), eq(null), any()))
                .thenThrow(new ValidationException("La cabecera Idempotency-Key es obligatoria"));

        mockMvc.perform(post("/api/operaciones/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void transferenciaUsaRutaEsperada() throws Exception {
        when(operacionService.transferir(any(TransferenciaRequestDTO.class), eq("tra-1"), any()))
                .thenReturn(response(TipoOperacion.TRANSFERENCIA, "tra-1"));

        mockMvc.perform(post("/api/operaciones/transferencia")
                        .header("Idempotency-Key", "tra-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoOperacion").value("TRANSFERENCIA"));
    }

    @Test
    void keyReutilizadaConOtroRequestDevuelve409() throws Exception {
        when(operacionService.depositar(any(OperacionRequestDTO.class), eq("same-key"), any()))
                .thenThrow(new IdempotencyConflictException("La Idempotency-Key ya fue usada para otra operacion"));

        mockMvc.perform(post("/api/operaciones/deposito")
                        .header("Idempotency-Key", "same-key")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.correlationId").value("corr-1"));
    }

    @Test
    void cuentaServiceNoDisponibleDevuelve503Controlado() throws Exception {
        when(operacionService.depositar(any(OperacionRequestDTO.class), eq("dep-down"), eq("corr-503")))
                .thenThrow(new RemoteServiceException("cuenta-service no esta disponible"));

        mockMvc.perform(post("/api/operaciones/deposito")
                        .header("Idempotency-Key", "dep-down")
                        .header("X-Correlation-Id", "corr-503")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CUENTA_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.correlationId").value("corr-503"));
    }

    private OperacionResponseDTO response(TipoOperacion tipoOperacion, String key) {
        return new OperacionResponseDTO(
                key,
                tipoOperacion,
                EstadoOperacion.COMPLETADA,
                List.of(new MovimientoResponseDTO(
                        1L,
                        10L,
                        "ES91210000000000000001",
                        tipoOperacion.name(),
                        new BigDecimal("50.00"),
                        LocalDateTime.now()
                ))
        );
    }
}
