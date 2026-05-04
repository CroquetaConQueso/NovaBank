package com.novabank.service;

import com.novabank.dto.CuentaCreateRequestDTO;
import com.novabank.dto.CuentaResponseDTO;
import com.novabank.dto.SaldoResponseDTO;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.mapper.CuentaMapper;
import com.novabank.model.Cliente;
import com.novabank.model.Cuenta;
import com.novabank.repository.ClienteRepository;
import com.novabank.repository.CuentaRepository;
import com.novabank.service.strategy.GeneradorNumeroCuentaStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.times;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;

    @Spy
    private CuentaMapper cuentaMapper;

    @InjectMocks
    private CuentaService cuentaService;

    @Test
    void crearCuentaAsociaClienteYSaldoInicialCero() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Ana")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
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
        assertThat(guardada.getCliente()).isSameAs(cliente);
        assertThat(guardada.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(guardada.getNumeroCuenta()).isEqualTo("ES91210000000000000001");
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.clienteId()).isEqualTo(1L);
    }

    @Test
    void crearCuentaLanzaErrorSiClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.crearCuenta(new CuentaCreateRequestDTO(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("cliente");
    }

    @Test
    void crearCuentaLanzaErrorSiClienteIdNoEsValido() {
        assertThatThrownBy(() -> cuentaService.crearCuenta(new CuentaCreateRequestDTO(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El id del cliente debe ser positivo");
    }

    @Test
    void obtenerCuentaPorNumeroCuandoExisteDevuelveResponse() {
        Cliente cliente = Cliente.builder().id(1L).nombre("Ana").build();
        Cuenta cuenta = Cuenta.builder()
                .id(10L)
                .cliente(cliente)
                .numeroCuenta("ES91210000000000000001")
                .saldo(new BigDecimal("50.00"))
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(cuentaRepository.findByNumeroCuenta("ES91210000000000000001")).thenReturn(Optional.of(cuenta));

        CuentaResponseDTO response = cuentaService.obtenerCuentaPorNumero("ES91210000000000000001");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.numeroCuenta()).isEqualTo("ES91210000000000000001");
        assertThat(response.clienteId()).isEqualTo(1L);
    }

    @Test
    void obtenerCuentaPorNumeroCuandoNoExisteLanzaResourceNotFound() {
        when(cuentaRepository.findByNumeroCuenta("ES91210000000000000001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.obtenerCuentaPorNumero("ES91210000000000000001"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("numero");
    }

    @Test
    void obtenerCuentaPorNumeroCuandoBlankLanzaValidationException() {
        assertThatThrownBy(() -> cuentaService.obtenerCuentaPorNumero("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El numero de cuenta es obligatorio");
    }

    @Test
    void obtenerCuentaPorNumeroNormalizaAntesDeBuscar() {
        when(cuentaRepository.findByNumeroCuenta("ES91210000000000000001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.obtenerCuentaPorNumero(" es91210000000000000001 "))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cuentaRepository).findByNumeroCuenta("ES91210000000000000001");
    }

    @Test
    void consultarSaldoCuandoExisteDevuelveSaldoResponseDTO() {
        Cliente cliente = Cliente.builder().id(1L).nombre("Ana").build();
        Cuenta cuenta = Cuenta.builder()
                .id(10L)
                .cliente(cliente)
                .numeroCuenta("ES91210000000000000001")
                .saldo(new BigDecimal("50.00"))
                .build();

        when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuenta));

        SaldoResponseDTO response = cuentaService.consultarSaldo(10L);

        assertThat(response.cuentaId()).isEqualTo(10L);
        assertThat(response.numeroCuenta()).isEqualTo("ES91210000000000000001");
        assertThat(response.saldo()).isEqualByComparingTo(new BigDecimal("50.00"));
        verify(cuentaRepository, times(1)).findById(10L);
    }

    @Test
    void consultarSaldoCuandoNoExisteLanzaResourceNotFound() {
        when(cuentaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.consultarSaldo(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("id");
    }
}
