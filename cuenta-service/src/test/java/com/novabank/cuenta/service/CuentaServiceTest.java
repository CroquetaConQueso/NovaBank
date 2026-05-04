package com.novabank.cuenta.service;

import com.novabank.cuenta.client.ClienteServiceClient;
import com.novabank.cuenta.dto.ClienteResponseDTO;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.MovimientoResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.exception.ValidationException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.mapper.MovimientoMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.Movimiento;
import com.novabank.cuenta.model.TipoMovimiento;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.repository.MovimientoRepository;
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
import java.util.concurrent.atomic.AtomicLong;

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
    private MovimientoRepository movimientoRepository;

    @Mock
    private ClienteServiceClient clienteServiceClient;

    @Mock
    private GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;

    @Spy
    private MovimientoFactory movimientoFactory;

    @Spy
    private CuentaMapper cuentaMapper;

    @Spy
    private MovimientoMapper movimientoMapper;

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
    void depositarAumentaSaldoYRegistraMovimiento() {
        Cuenta cuenta = cuenta(1L, "ES00000000000000000001", "100.00");
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(20L);
            return movimiento;
        });

        MovimientoResponseDTO response = cuentaService.depositar(
                1L,
                new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
        );

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("150.00");
        assertThat(response.tipo()).isEqualTo(TipoMovimiento.DEPOSITO);
        verify(movimientoRepository).save(any(Movimiento.class));
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
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void transferirActualizaAmbasCuentasYRegistraDosMovimientos() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "200.00");
        Cuenta destino = cuenta(2L, "ES91210000000000000002", "10.00");
        AtomicLong ids = new AtomicLong(30L);

        when(cuentaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(origen, destino));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(ids.getAndIncrement());
            return movimiento;
        });

        List<MovimientoResponseDTO> movimientos = cuentaService.transferir(
                new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("75.00"))
        );

        assertThat(origen.getSaldo()).isEqualByComparingTo("125.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("85.00");
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos.get(0).tipo()).isEqualTo(TipoMovimiento.TRANSFERENCIA_SALIENTE);
        assertThat(movimientos.get(1).tipo()).isEqualTo(TipoMovimiento.TRANSFERENCIA_ENTRANTE);
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
