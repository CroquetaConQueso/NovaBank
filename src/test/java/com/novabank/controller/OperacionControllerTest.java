package com.novabank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.dto.MovimientoResponseDTO;
import com.novabank.dto.OperacionRequestDTO;
import com.novabank.dto.TransferenciaRequestDTO;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.model.TipoMovimiento;
import com.novabank.service.OperacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OperacionService operacionService;

    @Test
    void depositoDevuelveMovimientoRegistrado() throws Exception {
        when(operacionService.depositar(any(OperacionRequestDTO.class))).thenReturn(
                new MovimientoResponseDTO(
                        20L,
                        10L,
                        "ES00000000000000000001",
                        TipoMovimiento.DEPOSITO,
                        new BigDecimal("100.00"),
                        LocalDateTime.now()
                )
        );

        OperacionRequestDTO request = new OperacionRequestDTO(
                "ES00000000000000000001",
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/operaciones/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("DEPOSITO"))
                .andExpect(jsonPath("$.cantidad").value(100.00));
    }

    @Test
    void retiroSinSaldoDevuelve422() throws Exception {
        when(operacionService.retirar(any(OperacionRequestDTO.class)))
                .thenThrow(new InsufficientBalanceException(
                        "Saldo insuficiente. Saldo disponible: 50.00 EUR. Importe solicitado: 100.00 EUR."
                ));

        OperacionRequestDTO request = new OperacionRequestDTO(
                "ES00000000000000000001",
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/operaciones/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SALDO_INSUFICIENTE"))
                .andExpect(jsonPath("$.message").value(
                        "Saldo insuficiente. Saldo disponible: 50.00 EUR. Importe solicitado: 100.00 EUR."
                ));
    }

    @Test
    void transferenciaDevuelveDosMovimientos() throws Exception {
        when(operacionService.transferir(any(TransferenciaRequestDTO.class))).thenReturn(List.of(
                new MovimientoResponseDTO(
                        20L,
                        10L,
                        "ES00000000000000000001",
                        TipoMovimiento.TRANSFERENCIA_SALIENTE,
                        new BigDecimal("50.00"),
                        LocalDateTime.now()
                ),
                new MovimientoResponseDTO(
                        21L,
                        11L,
                        "ES00000000000000000002",
                        TipoMovimiento.TRANSFERENCIA_ENTRANTE,
                        new BigDecimal("50.00"),
                        LocalDateTime.now()
                )
        ));

        TransferenciaRequestDTO request = new TransferenciaRequestDTO(
                "ES00000000000000000001",
                "ES00000000000000000002",
                new BigDecimal("50.00")
        );

        mockMvc.perform(post("/api/operaciones/transferencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("TRANSFERENCIA_SALIENTE"))
                .andExpect(jsonPath("$[1].tipo").value("TRANSFERENCIA_ENTRANTE"));
    }
}
