package com.novabank.cuenta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.MovimientoResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.GlobalExceptionHandler;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.model.TipoMovimiento;
import com.novabank.cuenta.service.CuentaService;
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

@WebMvcTest({CuentaController.class, InternalCuentaController.class})
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class CuentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CuentaService cuentaService;

    @Test
    void crearCuentaDevuelveCreatedYBody() throws Exception {
        when(cuentaService.crearCuenta(any(CuentaCreateRequestDTO.class))).thenReturn(cuentaResponse());

        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaCreateRequestDTO(1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.clienteId").value(1));
    }

    @Test
    void listarCuentasPorClienteUsaRutaEsperada() throws Exception {
        when(cuentaService.listarCuentasPorCliente(1L)).thenReturn(List.of(cuentaResponse()));

        mockMvc.perform(get("/api/cuentas/cliente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroCuenta").value("ES91210000000000000001"));
    }

    @Test
    void obtenerCuentaPorNumeroDevuelveCuenta() throws Exception {
        when(cuentaService.obtenerCuentaPorNumero("ES91210000000000000001")).thenReturn(cuentaResponse());

        mockMvc.perform(get("/api/cuentas/numero/ES91210000000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void consultarSaldoDevuelveSaldo() throws Exception {
        when(cuentaService.consultarSaldo(10L))
                .thenReturn(new SaldoResponseDTO(10L, "ES91210000000000000001", new BigDecimal("50.00")));

        mockMvc.perform(get("/api/cuentas/10/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(50.00));
    }

    @Test
    void listarMovimientosDevuelveArray() throws Exception {
        when(cuentaService.listarMovimientos(10L, null, null)).thenReturn(List.of(movimientoResponse()));

        mockMvc.perform(get("/api/cuentas/10/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("DEPOSITO"));
    }

    @Test
    void depositarInternoDevuelveMovimiento() throws Exception {
        when(cuentaService.depositar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(movimientoResponse());

        mockMvc.perform(post("/internal/cuentas/10/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaOperacionRequestDTO(new BigDecimal("50.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("DEPOSITO"));
    }

    @Test
    void retirarInternoConSaldoInsuficienteDevuelve422() throws Exception {
        when(cuentaService.retirar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new InsufficientBalanceException("Saldo insuficiente"));

        mockMvc.perform(post("/internal/cuentas/10/retiros")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaOperacionRequestDTO(new BigDecimal("50.00")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.correlationId").value("corr-1"));
    }

    @Test
    void transferirInternoDevuelveDosMovimientos() throws Exception {
        when(cuentaService.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(List.of(
                        movimiento(1L, TipoMovimiento.TRANSFERENCIA_SALIENTE),
                        movimiento(2L, TipoMovimiento.TRANSFERENCIA_ENTRANTE)
                ));

        mockMvc.perform(post("/internal/cuentas/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("25.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("TRANSFERENCIA_SALIENTE"))
                .andExpect(jsonPath("$[1].tipo").value("TRANSFERENCIA_ENTRANTE"));
    }

    @Test
    void cuentaNoEncontradaDevuelve404() throws Exception {
        when(cuentaService.obtenerCuenta(99L))
                .thenThrow(new ResourceNotFoundException("No existe ninguna cuenta con id 99"));

        mockMvc.perform(get("/api/cuentas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void clienteServiceNoDisponibleDevuelve503Controlado() throws Exception {
        when(cuentaService.crearCuenta(any(CuentaCreateRequestDTO.class)))
                .thenThrow(new RemoteServiceException("cliente-service no esta disponible"));

        mockMvc.perform(post("/api/cuentas")
                        .header("X-Correlation-Id", "corr-503")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaCreateRequestDTO(1L))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CLIENTE_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.correlationId").value("corr-503"));
    }

    private CuentaResponseDTO cuentaResponse() {
        return new CuentaResponseDTO(
                10L,
                "ES91210000000000000001",
                1L,
                new BigDecimal("50.00"),
                LocalDateTime.now()
        );
    }

    private MovimientoResponseDTO movimientoResponse() {
        return movimiento(20L, TipoMovimiento.DEPOSITO);
    }

    private MovimientoResponseDTO movimiento(Long id, TipoMovimiento tipo) {
        return new MovimientoResponseDTO(
                id,
                10L,
                "ES91210000000000000001",
                tipo,
                new BigDecimal("50.00"),
                LocalDateTime.now()
        );
    }
}
