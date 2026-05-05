package com.novabank.operacion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.GlobalExceptionHandler;
import com.novabank.operacion.exception.RemoteServiceException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void depositoDevuelveOperacionRealizada() throws Exception {
        when(operacionService.depositar(any(OperacionRequestDTO.class)))
                .thenReturn(response("DEPOSITO"));

        mockMvc.perform(post("/api/operaciones/deposito")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoOperacion").value("DEPOSITO"))
                .andExpect(jsonPath("$.mensaje").value("Operacion realizada correctamente"));
    }

    @Test
    void retiroSinCabecerasEspecialesDevuelveOperacionRealizada() throws Exception {
        when(operacionService.retirar(any(OperacionRequestDTO.class)))
                .thenReturn(response("RETIRO"));

        mockMvc.perform(post("/api/operaciones/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoOperacion").value("RETIRO"));
    }

    @Test
    void transferenciaUsaRutaEsperada() throws Exception {
        when(operacionService.transferir(any(TransferenciaRequestDTO.class)))
                .thenReturn(response("TRANSFERENCIA"));

        mockMvc.perform(post("/api/operaciones/transferencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoOperacion").value("TRANSFERENCIA"));
    }

    @Test
    void listarMovimientosUsaRutaDeOperacionService() throws Exception {
        when(operacionService.listarMovimientos(10L, null, null))
                .thenReturn(List.of(movimiento("DEPOSITO")));

        mockMvc.perform(get("/api/operaciones/cuentas/10/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("DEPOSITO"));
    }

    @Test
    void cuentaServiceNoDisponibleDevuelve503Controlado() throws Exception {
        when(operacionService.depositar(any(OperacionRequestDTO.class)))
                .thenThrow(new RemoteServiceException("cuenta-service no esta disponible"));

        mockMvc.perform(post("/api/operaciones/deposito")
                        .header("X-Correlation-Id", "corr-503")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CUENTA_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.correlationId").value("corr-503"));
    }

    private OperacionResponseDTO response(String tipoOperacion) {
        return new OperacionResponseDTO(
                tipoOperacion,
                "Operacion realizada correctamente",
                List.of(movimiento(tipoOperacion))
        );
    }

    private MovimientoResponseDTO movimiento(String tipoOperacion) {
        return new MovimientoResponseDTO(
                        1L,
                        10L,
                        "ES91210000000000000001",
                        tipoOperacion,
                        new BigDecimal("50.00"),
                        LocalDateTime.now()
        );
    }
}
