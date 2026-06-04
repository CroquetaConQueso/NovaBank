package com.novabank.cuenta.service;

import com.novabank.cuenta.client.ClienteServiceClient;
import com.novabank.cuenta.dto.ClienteResponseDTO;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.event.MovimientoRegistradoEventPublisher;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private MovimientoRegistradoEventPublisher movimientoRegistradoEventPublisher;

    @Mock
    private SaldoBajoAlertService saldoBajoAlertService;

    @InjectMocks
    private CuentaService cuentaService;

    @Test
    void crearCuentaValidaClienteYSaldoInicialCero() {
        when(clienteServiceClient.obtenerCliente(1L)).thenReturn(Mono.just(cliente(1L)));
        when(generadorNumeroCuentaStrategy.generarNumeroCuenta()).thenReturn(Mono.just("ES91210000000000000001"));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> {
            Cuenta cuenta = invocation.getArgument(0);
            cuenta.setId(10L);
            cuenta.setFechaCreacion(LocalDateTime.now());
            return Mono.just(cuenta);
        });

        StepVerifier.create(cuentaService.crearCuenta(new CuentaCreateRequestDTO(1L)))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(10L);
                    assertThat(response.clienteId()).isEqualTo(1L);
                })
                .verifyComplete();

        ArgumentCaptor<Cuenta> captor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(captor.capture());

        Cuenta guardada = captor.getValue();
        assertThat(guardada.getClienteId()).isEqualTo(1L);
        assertThat(guardada.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(guardada.getNumeroCuenta()).isEqualTo("ES91210000000000000001");
    }

    @Test
    void crearCuentaPropagaClienteNoEncontradoDelClienteReactivo() {
        when(clienteServiceClient.obtenerCliente(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ningun cliente con el id indicado")));

        StepVerifier.create(cuentaService.crearCuenta(new CuentaCreateRequestDTO(99L)))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void crearCuentaCuandoClienteServiceNoDisponibleNoGuardaCuenta() {
        when(clienteServiceClient.obtenerCliente(1L))
                .thenReturn(Mono.error(new RemoteServiceException("cliente-service no esta disponible")));

        StepVerifier.create(cuentaService.crearCuenta(new CuentaCreateRequestDTO(1L)))
                .expectError(RemoteServiceException.class)
                .verify();

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void crearCuentaConClienteIdNuloLanzaValidationExceptionSinLlamadaRemota() {
        StepVerifier.create(cuentaService.crearCuenta(new CuentaCreateRequestDTO(null)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("El id del cliente debe ser positivo");
                })
                .verify();

        verify(clienteServiceClient, never()).obtenerCliente(any());
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void listarCuentasPorClientePropagaClienteNoEncontrado() {
        when(clienteServiceClient.obtenerCliente(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ningun cliente con el id indicado")));

        StepVerifier.create(cuentaService.listarCuentasPorCliente(99L))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(cuentaRepository, never()).findByClienteId(any());
    }

    @Test
    void obtenerCuentaPorNumeroNormalizaAntesDeBuscar() {
        when(cuentaRepository.findByNumeroCuenta("ES91210000000000000001")).thenReturn(Mono.empty());

        StepVerifier.create(cuentaService.obtenerCuentaPorNumero(" es91210000000000000001 "))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(cuentaRepository).findByNumeroCuenta("ES91210000000000000001");
    }

    @Test
    void obtenerCuentaPorNumeroBlankLanzaValidationException() {
        StepVerifier.create(cuentaService.obtenerCuentaPorNumero("   "))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ValidationException.class);
                    assertThat(error).hasMessage("El numero de cuenta es obligatorio");
                })
                .verify();
    }

    @Test
    void consultarSaldoCuandoExisteDevuelveSaldoResponseDTO() {
        Cuenta cuenta = cuenta(10L, "ES91210000000000000001", "50.00");
        when(cuentaRepository.findById(10L)).thenReturn(Mono.just(cuenta));

        StepVerifier.create(cuentaService.consultarSaldo(10L))
                .assertNext(response -> {
                    assertThat(response.cuentaId()).isEqualTo(10L);
                    assertThat(response.numeroCuenta()).isEqualTo("ES91210000000000000001");
                    assertThat(response.saldo()).isEqualByComparingTo("50.00");
                })
                .verifyComplete();
    }

    @Test
    void consultarSaldoCuentaInexistenteLanza404() {
        when(cuentaRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(cuentaService.consultarSaldo(99L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                    assertThat(error).hasMessageContaining("99");
                })
                .verify();
    }

    @Test
    void depositarAumentaSaldoYDevuelveCuentaActualizada() {
        Cuenta cuenta = cuenta(1L, "ES00000000000000000001", "100.00");
        when(cuentaRepository.findById(1L)).thenReturn(Mono.just(cuenta));
        when(cuentaRepository.save(cuenta)).thenReturn(Mono.just(cuenta));
        when(movimientoRegistradoEventPublisher.publicar(any())).thenReturn(Mono.empty());
        when(saldoBajoAlertService.evaluarYPublicar(any())).thenReturn(Mono.empty());

        StepVerifier.create(cuentaService.depositar(
                        1L,
                        new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
                ))
                .assertNext(response -> assertThat(response.saldo()).isEqualByComparingTo("150.00"))
                .verifyComplete();

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("150.00");
        verify(movimientoRegistradoEventPublisher).publicar(any());
        verify(saldoBajoAlertService).evaluarYPublicar(any());
    }

    @Test
    void depositarCantidadCeroLanzaValidationException() {
        StepVerifier.create(cuentaService.depositar(
                        1L,
                        new CuentaOperacionRequestDTO(BigDecimal.ZERO)
                ))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("La cantidad debe ser mayor que cero");
                })
                .verify();

        verify(cuentaRepository, never()).findById(eq(1L));
        verify(movimientoRegistradoEventPublisher, never()).publicar(any());
        verify(saldoBajoAlertService, never()).evaluarYPublicar(any());
    }

    @Test
    void retirarCantidadNegativaLanzaValidationException() {
        StepVerifier.create(cuentaService.retirar(
                        1L,
                        new CuentaOperacionRequestDTO(new BigDecimal("-1.00"))
                ))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("La cantidad debe ser mayor que cero");
                })
                .verify();

        verify(cuentaRepository, never()).findById(eq(1L));
    }

    @Test
    void retirarLanzaErrorSiSaldoInsuficiente() {
        Cuenta cuenta = cuenta(1L, "ES00000000000000000001", "25.00");
        when(cuentaRepository.findById(1L)).thenReturn(Mono.just(cuenta));

        StepVerifier.create(cuentaService.retirar(
                        1L,
                        new CuentaOperacionRequestDTO(new BigDecimal("50.00"))
                ))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(InsufficientBalanceException.class);
                    assertThat(error).hasMessage("Saldo insuficiente. Saldo disponible: 25.00 EUR. Importe solicitado: 50.00 EUR.");
                })
                .verify();

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("25.00");
        verify(movimientoRegistradoEventPublisher, never()).publicar(any());
        verify(saldoBajoAlertService, never()).evaluarYPublicar(any());
    }

    @Test
    void transferirActualizaAmbasCuentasYDevuelveCuentasActualizadas() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "200.00");
        Cuenta destino = cuenta(2L, "ES91210000000000000002", "10.00");

        when(cuentaRepository.findAllById(List.of(1L, 2L))).thenReturn(Flux.just(origen, destino));
        when(cuentaRepository.saveAll(any(Iterable.class))).thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));
        when(movimientoRegistradoEventPublisher.publicar(any())).thenReturn(Mono.empty());
        when(saldoBajoAlertService.evaluarYPublicar(any())).thenReturn(Mono.empty());

        StepVerifier.create(cuentaService.transferir(
                        new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("75.00"))
                ))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(1L);
                    assertThat(response.saldo()).isEqualByComparingTo("125.00");
                })
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(2L);
                    assertThat(response.saldo()).isEqualByComparingTo("85.00");
                })
                .verifyComplete();

        assertThat(origen.getSaldo()).isEqualByComparingTo("125.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("85.00");
        verify(movimientoRegistradoEventPublisher, org.mockito.Mockito.times(2)).publicar(any());
        verify(saldoBajoAlertService, org.mockito.Mockito.times(2)).evaluarYPublicar(any());
    }

    @Test
    void transferirMismaCuentaLanzaValidationExceptionSinBuscarCuentas() {
        StepVerifier.create(cuentaService.transferir(
                        new TransferenciaInternaRequestDTO(1L, 1L, new BigDecimal("10.00"))
                ))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("La cuenta origen y destino deben ser diferentes");
                })
                .verify();

        verify(cuentaRepository, never()).findAllById(any(Iterable.class));
    }

    @Test
    void transferirConCuentaDestinoInexistenteLanza404YNoDejaSaldoNegativo() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "200.00");
        when(cuentaRepository.findAllById(List.of(1L, 99L))).thenReturn(Flux.just(origen));

        StepVerifier.create(cuentaService.transferir(
                        new TransferenciaInternaRequestDTO(1L, 99L, new BigDecimal("75.00"))
                ))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                    assertThat(error).hasMessageContaining("99");
                })
                .verify();

        assertThat(origen.getSaldo()).isEqualByComparingTo("200.00");
    }

    @Test
    void transferirConSaldoInsuficienteNoModificaSaldos() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "10.00");
        Cuenta destino = cuenta(2L, "ES91210000000000000002", "20.00");
        when(cuentaRepository.findAllById(List.of(1L, 2L))).thenReturn(Flux.just(origen, destino));

        StepVerifier.create(cuentaService.transferir(
                        new TransferenciaInternaRequestDTO(1L, 2L, new BigDecimal("75.00"))
                ))
                .expectError(InsufficientBalanceException.class)
                .verify();

        assertThat(origen.getSaldo()).isEqualByComparingTo("10.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("20.00");
        verify(movimientoRegistradoEventPublisher, never()).publicar(any());
        verify(saldoBajoAlertService, never()).evaluarYPublicar(any());
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
