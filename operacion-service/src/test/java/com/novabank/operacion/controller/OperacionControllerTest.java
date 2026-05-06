package com.novabank.operacion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.GlobalExceptionHandler;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import com.novabank.operacion.exception.ValidationException;
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
import java.time.LocalDate;
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
                        )))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CUENTA_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.service").value("operacion-service"));
    }

    @Test
    void depositoConBodyVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/operaciones/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.cuentaId").exists())
                .andExpect(jsonPath("$.fieldErrors.cantidad").exists());
    }

    @Test
    void depositoConJsonMalformadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/operaciones/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void depositoConCuentaIdNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/operaciones/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(-1L, new BigDecimal("10.00"))
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void depositoConCantidadCeroDevuelve400() throws Exception {
        mockMvc.perform(post("/api/operaciones/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, BigDecimal.ZERO)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void retiroConCantidadNegativaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/operaciones/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("-1.00"))
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transferenciaConCuentaOrigenNulaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/operaciones/transferencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaRequestDTO(null, 11L, new BigDecimal("10.00"))
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.cuentaOrigenId").exists());
    }

    @Test
    void transferenciaConCantidadCeroDevuelve400() throws Exception {
        mockMvc.perform(post("/api/operaciones/transferencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaRequestDTO(10L, 11L, BigDecimal.ZERO)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listarMovimientosSinResultadosDevuelveListaVacia() throws Exception {
        when(operacionService.listarMovimientos(10L, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/operaciones/cuentas/10/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listarMovimientosConCuentaIdInvalidoDevuelve400() throws Exception {
        when(operacionService.listarMovimientos(0L, null, null))
                .thenThrow(new ValidationException("El id de cuenta debe ser positivo"));

        mockMvc.perform(get("/api/operaciones/cuentas/0/movimientos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void listarMovimientosConRangoInvalidoDevuelve400() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 1, 10);
        LocalDate fin = LocalDate.of(2026, 1, 1);
        when(operacionService.listarMovimientos(10L, inicio, fin))
                .thenThrow(new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin"));

        mockMvc.perform(get("/api/operaciones/cuentas/10/movimientos")
                        .param("fechaInicio", "2026-01-10")
                        .param("fechaFin", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void depositoConCuentaNoEncontradaDevuelve404() throws Exception {
        when(operacionService.depositar(any(OperacionRequestDTO.class)))
                .thenThrow(new RemoteResourceNotFoundException("Cuenta no encontrada"));

        mockMvc.perform(post("/api/operaciones/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(99L, new BigDecimal("10.00"))
                )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void retiroConSaldoInsuficienteRemotoDevuelve422() throws Exception {
        when(operacionService.retirar(any(OperacionRequestDTO.class)))
                .thenThrow(new RemoteValidationException("Saldo insuficiente"));

        mockMvc.perform(post("/api/operaciones/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(10L, new BigDecimal("100.00"))
                        )))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REMOTE_VALIDATION_ERROR"));
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
