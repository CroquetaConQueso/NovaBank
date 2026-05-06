package com.novabank.cuenta.service;

import com.novabank.cuenta.client.ClienteServiceClient;
import com.novabank.cuenta.dto.ClienteResponseDTO;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.exception.ValidationException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.service.strategy.GeneradorNumeroCuentaStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteServiceClient clienteServiceClient;

    @Mock
    private GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;

    @Spy
    private CuentaMapper cuentaMapper;

    @InjectMocks
    private CuentaService cuentaService;

    @Test
    void crearCuentaValidaClienteYSaldoInicialCero() {
        when(clienteServiceClient.obtenerCliente(1L)).thenReturn(cliente(1L));
        when(generadorNumeroCuentaStrategy.generarNumeroCuenta()).thenReturn("ES91210000000000000001");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> {
            Cuenta cuenta = invocation.getArgument(0);
            cuenta.setId(10L);
            cuenta.setFechaCreacion(LocalDateTime.now());
            return cuenta;
        });

        CuentaResponseDTO response = cuentaService.crearCuenta(new CuentaCreateRequestDTO(1L));

        ArgumentCaptor<Cuenta> captor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(captor.capture());

        Cuenta guardada = captor.getValue();
        assertThat(guardada.getClienteId()).isEqualTo(1L);
        assertThat(guardada.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(guardada.getNumeroCuenta()).isEqualTo("ES91210000000000000001");
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.clienteId()).isEqualTo(1L);
    }

    @Test
    void crearCuentaPropagaClienteNoEncontradoDeFeignDecoder() {
        when(clienteServiceClient.obtenerCliente(99L))
                .thenThrow(new ResourceNotFoundException("No existe ningun cliente con el id indicado"));

        assertThatThrownBy(() -> cuentaService.crearCuenta(new CuentaCreateRequestDTO(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("cliente");

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void crearCuentaCuandoClienteServiceNoDisponibleNoGuardaCuenta() {
        when(clienteServiceClient.obtenerCliente(1L))
                .thenThrow(new RemoteServiceException("cliente-service no esta disponible"));

        assertThatThrownBy(() -> cuentaService.crearCuenta(new CuentaCreateRequestDTO(1L)))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cliente-service");

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void crearCuentaConClienteIdNuloLanzaValidationExceptionSinLlamadaRemota() {
        assertThatThrownBy(() -> cuentaService.crearCuenta(new CuentaCreateRequestDTO(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El id del cliente debe ser positivo");

        verify(clienteServiceClient, never()).obtenerCliente(any());
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void listarCuentasPorClientePropagaClienteNoEncontrado() {
        when(clienteServiceClient.obtenerCliente(99L))
                .thenThrow(new ResourceNotFoundException("No existe ningun cliente con el id indicado"));

        assertThatThrownBy(() -> cuentaService.listarCuentasPorCliente(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cuentaRepository, never()).findByClienteId(any());
    }

    @Test
    void obtenerCuentaPorNumeroNormalizaAntesDeBuscar() {
        when(cuentaRepository.findByNumeroCuenta("ES91210000000000000001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.obtenerCuentaPorNumero(" es91210000000000000001 "))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cuentaRepository).findByNumeroCuenta("ES91210000000000000001");
    }

    @Test
    void obtenerCuentaPorNumeroBlankLanzaValidationException() {
        assertThatThrownBy(() -> cuentaService.obtenerCuentaPorNumero("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El numero de cuenta es obligatorio");
    }

    @Test
    void consultarSaldoCuandoExisteDevuelveSaldoResponseDTO() {
        Cuenta cuenta = cuenta(10L, "ES91210000000000000001", "50.00");
        when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuenta));

        SaldoResponseDTO response = cuentaService.consultarSaldo(10L);

        assertThat(response.cuentaId()).isEqualTo(10L);
        assertThat(response.numeroCuenta()).isEqualTo("ES91210000000000000001");
        assertThat(response.saldo()).isEqualByComparingTo("50.00");
    }

    @Test
    void consultarSaldoCuentaInexistenteLanza404() {
        when(cuentaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.consultarSaldo(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void depositarAumentaSaldoYDevuelveCuentaActualizada() {
        Cuenta cuenta = cuenta(1L, "ES00000000000000000001", "100.00");
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuenta));

        CuentaResponseDTO response = cuentaService.depositar(
                1L,
                new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
        );

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("150.00");
        assertThat(response.saldo()).isEqualByComparingTo("150.00");
    }

    @Test
    void depositarCantidadCeroLanzaValidationException() {
        assertThatThrownBy(() -> cuentaService.depositar(
                1L,
                new CuentaOperacionRequestDTO(BigDecimal.ZERO)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cantidad debe ser mayor que cero");

        verify(cuentaRepository, never()).findById(any());
    }

    @Test
    void retirarCantidadNegativaLanzaValidationException() {
        assertThatThrownBy(() -> cuentaService.retirar(
                1L,
                new CuentaOperacionRequestDTO(new BigDecimal("-1.00"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cantidad debe ser mayor que cero");

        verify(cuentaRepository, never()).findById(any());
    }

    @Test
    void retirarLanzaErrorSiSaldoInsuficiente() {
        Cuenta cuenta = cuenta(1L, "ES00000000000000000001", "25.00");
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaService.retirar(
                1L,
                new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
        ))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Saldo insuficiente. Saldo disponible: 25.00 EUR. Importe solicitado: 50.00 EUR.");

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("25.00");
    }

    @Test
    void transferirActualizaAmbasCuentasYDevuelveCuentasActualizadas() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "200.00");
        Cuenta destino = cuenta(2L, "ES91210000000000000002", "10.00");

        when(cuentaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(origen, destino));

        List<CuentaResponseDTO> cuentas = cuentaService.transferir(
                new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("75.00"))
        );

        assertThat(origen.getSaldo()).isEqualByComparingTo("125.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("85.00");
        assertThat(cuentas).hasSize(2);
        assertThat(cuentas.get(0).id()).isEqualTo(1L);
        assertThat(cuentas.get(1).id()).isEqualTo(2L);
    }

    @Test
    void transferirMismaCuentaLanzaValidationExceptionSinBuscarCuentas() {
        assertThatThrownBy(() -> cuentaService.transferir(
                new TransferenciaInternaRequestDTO(1L, 1L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cuenta origen y destino deben ser diferentes");

        verify(cuentaRepository, never()).findAllById(any());
    }

    @Test
    void transferirConCuentaDestinoInexistenteLanza404YNoDejaSaldoNegativo() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "200.00");
        when(cuentaRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(origen));

        assertThatThrownBy(() -> cuentaService.transferir(
                new TransferenciaInternaRequestDTO(1L, 99L, new BigDecimal("75.00"))
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        assertThat(origen.getSaldo()).isEqualByComparingTo("200.00");
    }

    @Test
    void transferirConSaldoInsuficienteNoModificaSaldos() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "10.00");
        Cuenta destino = cuenta(2L, "ES91210000000000000002", "20.00");
        when(cuentaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(origen, destino));

        assertThatThrownBy(() -> cuentaService.transferir(
                new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("75.00"))
        ))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(origen.getSaldo()).isEqualByComparingTo("10.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("20.00");
    }

    private ClienteResponseDTO cliente(Long id) {
        return new ClienteResponseDTO(id, "Ana", "Garcia", "12345678Z", "ana@example.com", "600111222", LocalDateTime.now());
    }

    private Cuenta cuenta(Long id, String numeroCuenta, String saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(id);
        cuenta.setClienteId(1L);
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.setFechaCreacion(LocalDateTime.now());
        return cuenta;
    }
}
