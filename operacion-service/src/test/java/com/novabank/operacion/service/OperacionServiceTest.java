package com.novabank.operacion.service;

import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionServiceTest {

    private CuentaServiceClient cuentaServiceClient;
    private OperacionService service;

    @BeforeEach
    void setUp() {
        cuentaServiceClient = mock(CuentaServiceClient.class);
        service = new OperacionService(cuentaServiceClient);
    }

    @Test
    void depositoCorrectoDelegaEnCuentaService() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(movimiento("DEPOSITO"));

        OperacionResponseDTO response = service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
        );

        assertThat(response.tipoOperacion()).isEqualTo("DEPOSITO");
        assertThat(response.mensaje()).isEqualTo("Deposito realizado correctamente");
        assertThat(response.movimientos()).hasSize(1);
        verify(cuentaServiceClient).depositar(eq(10L), any(CuentaOperacionRequestDTO.class));
    }

    @Test
    void retiroCorrectoDelegaEnCuentaService() {
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(movimiento("RETIRO"));

        OperacionResponseDTO response = service.retirar(
                new OperacionRequestDTO(10L, new BigDecimal("25.00"))
        );

        assertThat(response.tipoOperacion()).isEqualTo("RETIRO");
        assertThat(response.movimientos()).hasSize(1);
        verify(cuentaServiceClient).retirar(eq(10L), any(CuentaOperacionRequestDTO.class));
    }

    @Test
    void transferenciaCorrectaUsaEndpointInternoUnico() {
        when(cuentaServiceClient.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(List.of(movimiento("TRANSFERENCIA_SALIENTE"), movimiento("TRANSFERENCIA_ENTRANTE")));

        OperacionResponseDTO response = service.transferir(
                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("25.00"))
        );

        assertThat(response.tipoOperacion()).isEqualTo("TRANSFERENCIA");
        assertThat(response.movimientos()).hasSize(2);
        verify(cuentaServiceClient).transferir(any(TransferenciaInternaRequestDTO.class));
        verify(cuentaServiceClient, never()).retirar(any(), any());
        verify(cuentaServiceClient, never()).depositar(any(), any());
    }

    @Test
    void saldoInsuficientePropagaErrorControlado() {
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new RemoteValidationException("Saldo insuficiente"));

        assertThatThrownBy(() -> service.retirar(
                new OperacionRequestDTO(10L, new BigDecimal("999.00"))
        ))
                .isInstanceOf(RemoteValidationException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void cuentaServiceNoDisponibleEnDepositoPropagaServicioNoDisponible() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new RemoteServiceException("cuenta-service no esta disponible"));

        assertThatThrownBy(() -> service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
        ))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cuenta-service no esta disponible");
    }

    @Test
    void cuentaServiceNoDisponibleEnTransferenciaPropagaServicioNoDisponible() {
        when(cuentaServiceClient.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenThrow(new RemoteServiceException("cuenta-service no esta disponible"));

        assertThatThrownBy(() -> service.transferir(
                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00"))
        ))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cuenta-service no esta disponible");
    }

    private MovimientoResponseDTO movimiento(String tipo) {
        return new MovimientoResponseDTO(
                1L,
                10L,
                "ES91210000000000000001",
                tipo,
                new BigDecimal("50.00"),
                LocalDateTime.now()
        );
    }
}
