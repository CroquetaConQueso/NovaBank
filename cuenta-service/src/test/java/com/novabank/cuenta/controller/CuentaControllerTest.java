package com.novabank.cuenta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.GlobalExceptionHandler;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
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
    void obtenerCuentaPorIdDevuelveCuenta() throws Exception {
        when(cuentaService.obtenerCuenta(10L)).thenReturn(cuentaResponse());

        mockMvc.perform(get("/api/cuentas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.numeroCuenta").value("ES91210000000000000001"));
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
    void depositarInternoDevuelveCuentaActualizada() throws Exception {
        when(cuentaService.depositar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(cuentaResponse("100.00"));

        mockMvc.perform(post("/internal/cuentas/10/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaOperacionRequestDTO(new BigDecimal("50.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(100.00));
    }

    @Test
    void depositarCantidadDecimalMinimaDevuelveCuentaActualizada() throws Exception {
        when(cuentaService.depositar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(cuentaResponse("50.01"));

        mockMvc.perform(post("/internal/cuentas/10/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaOperacionRequestDTO(new BigDecimal("0.01")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(50.01));
    }

    @Test
    void retirarInternoConSaldoInsuficienteDevuelve422() throws Exception {
        when(cuentaService.retirar(any(Long.class), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new InsufficientBalanceException("Saldo insuficiente"));

        mockMvc.perform(post("/internal/cuentas/10/retiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaOperacionRequestDTO(new BigDecimal("50.00")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.service").value("cuenta-service"));
    }

    @Test
    void transferirInternoDevuelveDosCuentasActualizadas() throws Exception {
        when(cuentaService.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(List.of(
                        cuentaResponse(1L, "ES91210000000000000001", "25.00"),
                        cuentaResponse(2L, "ES91210000000000000002", "75.00")
                ));

        mockMvc.perform(post("/internal/cuentas/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("25.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaCreateRequestDTO(1L))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CLIENTE_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.service").value("cuenta-service"));
    }

    @Test
    void crearCuentaConClienteIdNuloDevuelve400() throws Exception {
        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.clienteId").exists());
    }

    @Test
    void crearCuentaConClienteIdNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaCreateRequestDTO(-1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.clienteId").exists());
    }

    @Test
    void crearCuentaConJsonMalformadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void listarCuentasDeClienteInexistenteDevuelve404() throws Exception {
        when(cuentaService.listarCuentasPorCliente(99L))
                .thenThrow(new ResourceNotFoundException("No existe ningun cliente con id 99"));

        mockMvc.perform(get("/api/cuentas/cliente/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void obtenerCuentaPorNumeroInexistenteDevuelve404() throws Exception {
        when(cuentaService.obtenerCuentaPorNumero("ES91210000000000000999"))
                .thenThrow(new ResourceNotFoundException("No existe ninguna cuenta con numero ES91210000000000000999"));

        mockMvc.perform(get("/api/cuentas/numero/ES91210000000000000999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void consultarSaldoCuentaInexistenteDevuelve404() throws Exception {
        when(cuentaService.consultarSaldo(99L))
                .thenThrow(new ResourceNotFoundException("No existe ninguna cuenta con id 99"));

        mockMvc.perform(get("/api/cuentas/99/saldo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void depositarCantidadCeroDevuelve400() throws Exception {
        mockMvc.perform(post("/internal/cuentas/10/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaOperacionRequestDTO(BigDecimal.ZERO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void retirarCantidadNegativaDevuelve400() throws Exception {
        mockMvc.perform(post("/internal/cuentas/10/retiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaOperacionRequestDTO(new BigDecimal("-1.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transferirMismaCuentaDevuelve400() throws Exception {
        when(cuentaService.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("La cuenta origen y destino deben ser distintas"));

        mockMvc.perform(post("/internal/cuentas/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaInternaRequestDTO(10L, 10L, new BigDecimal("5.00"))
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void transferirCuentaDestinoInexistenteDevuelve404() throws Exception {
        when(cuentaService.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("No existe ninguna cuenta con id 99"));

        mockMvc.perform(post("/internal/cuentas/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaInternaRequestDTO(10L, 99L, new BigDecimal("5.00"))
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void transferirCantidadCeroDevuelve400() throws Exception {
        mockMvc.perform(post("/internal/cuentas/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaInternaRequestDTO(10L, 11L, BigDecimal.ZERO)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private CuentaResponseDTO cuentaResponse() {
        return cuentaResponse("50.00");
    }

    private CuentaResponseDTO cuentaResponse(String saldo) {
        return cuentaResponse(10L, "ES91210000000000000001", saldo);
    }

    private CuentaResponseDTO cuentaResponse(Long id, String numeroCuenta, String saldo) {
        return new CuentaResponseDTO(
                id,
                numeroCuenta,
                1L,
                new BigDecimal(saldo),
                LocalDateTime.now()
        );
    }
}
