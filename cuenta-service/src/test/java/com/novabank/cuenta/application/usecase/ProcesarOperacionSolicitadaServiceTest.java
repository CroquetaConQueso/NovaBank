package com.novabank.cuenta.application.usecase;

import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaCommand;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaResultado;
import com.novabank.cuenta.application.port.out.AplicarOperacionCuentaPort;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesarOperacionSolicitadaServiceTest {

    @Mock
    private AplicarOperacionCuentaPort aplicarOperacionCuentaPort;

    @Test
    void procesarDepositoValidoDevuelveResultadoCompletado() {
        ProcesarOperacionSolicitadaService service = service();
        ProcesarOperacionSolicitadaCommand command = operacion("DEPOSITO", null, 10L, "25.00");

        when(aplicarOperacionCuentaPort.depositar(eq(10L), eq(new BigDecimal("25.00")))).thenReturn(Mono.empty());

        StepVerifier.create(service.procesar(command))
                .assertNext(resultado -> {
                    assertThat(resultado.estado()).isEqualTo(ProcesarOperacionSolicitadaResultado.Estado.COMPLETADA);
                    assertThat(resultado.operationId()).isEqualTo(command.operationId());
                    assertThat(resultado.tipoOperacion()).isEqualTo("DEPOSITO");
                    assertThat(resultado.codigoError()).isNull();
                    assertThat(resultado.motivo()).isNull();
                })
                .verifyComplete();

        verify(aplicarOperacionCuentaPort).depositar(10L, new BigDecimal("25.00"));
    }

    @Test
    void procesarRetiradaValidaDevuelveResultadoCompletado() {
        ProcesarOperacionSolicitadaService service = service();
        ProcesarOperacionSolicitadaCommand command = operacion("RETIRADA", 10L, null, "25.00");

        when(aplicarOperacionCuentaPort.retirar(eq(10L), eq(new BigDecimal("25.00")))).thenReturn(Mono.empty());

        StepVerifier.create(service.procesar(command))
                .assertNext(resultado -> assertThat(resultado.estado())
                        .isEqualTo(ProcesarOperacionSolicitadaResultado.Estado.COMPLETADA))
                .verifyComplete();

        verify(aplicarOperacionCuentaPort).retirar(10L, new BigDecimal("25.00"));
    }

    @Test
    void procesarTransferenciaValidaDevuelveResultadoCompletado() {
        ProcesarOperacionSolicitadaService service = service();
        ProcesarOperacionSolicitadaCommand command = operacion("TRANSFERENCIA", 10L, 11L, "25.00");

        when(aplicarOperacionCuentaPort.transferir(eq(10L), eq(11L), eq(new BigDecimal("25.00"))))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.procesar(command))
                .assertNext(resultado -> assertThat(resultado.estado())
                        .isEqualTo(ProcesarOperacionSolicitadaResultado.Estado.COMPLETADA))
                .verifyComplete();

        verify(aplicarOperacionCuentaPort).transferir(10L, 11L, new BigDecimal("25.00"));
    }

    @Test
    void procesarSaldoInsuficienteDevuelveResultadoFallidoConCodigoDeNegocio() {
        ProcesarOperacionSolicitadaService service = service();
        ProcesarOperacionSolicitadaCommand command = operacion("RETIRADA", 10L, null, "999.00");

        when(aplicarOperacionCuentaPort.retirar(eq(10L), eq(new BigDecimal("999.00"))))
                .thenReturn(Mono.error(new InsufficientBalanceException("Saldo insuficiente")));

        StepVerifier.create(service.procesar(command))
                .assertNext(resultado -> {
                    assertThat(resultado.estado()).isEqualTo(ProcesarOperacionSolicitadaResultado.Estado.FALLIDA);
                    assertThat(resultado.codigoError()).isEqualTo("SALDO_INSUFICIENTE");
                    assertThat(resultado.motivo()).startsWith("Saldo insuficiente");
                })
                .verifyComplete();
    }

    @Test
    void procesarCuentaInexistenteDevuelveResultadoFallidoConCodigoDeNegocio() {
        ProcesarOperacionSolicitadaService service = service();
        ProcesarOperacionSolicitadaCommand command = operacion("DEPOSITO", null, 99L, "25.00");

        when(aplicarOperacionCuentaPort.depositar(eq(99L), eq(new BigDecimal("25.00"))))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ninguna cuenta con id 99")));

        StepVerifier.create(service.procesar(command))
                .assertNext(resultado -> {
                    assertThat(resultado.estado()).isEqualTo(ProcesarOperacionSolicitadaResultado.Estado.FALLIDA);
                    assertThat(resultado.codigoError()).isEqualTo("CUENTA_NO_ENCONTRADA");
                    assertThat(resultado.motivo()).startsWith("No existe ninguna cuenta");
                })
                .verifyComplete();
    }

    @Test
    void procesarPayloadInvalidoDevuelveSolicitudInvalidaSinAplicarOperacion() {
        ProcesarOperacionSolicitadaService service = service();
        ProcesarOperacionSolicitadaCommand command = operacion("DEPOSITO", null, null, "25.00");

        StepVerifier.create(service.procesar(command))
                .assertNext(resultado -> {
                    assertThat(resultado.estado()).isEqualTo(ProcesarOperacionSolicitadaResultado.Estado.FALLIDA);
                    assertThat(resultado.codigoError()).isEqualTo("SOLICITUD_INVALIDA");
                    assertThat(resultado.motivo()).startsWith("La cuenta destino es obligatoria");
                })
                .verifyComplete();

        verify(aplicarOperacionCuentaPort, never()).depositar(any(), any());
        verify(aplicarOperacionCuentaPort, never()).retirar(any(), any());
        verify(aplicarOperacionCuentaPort, never()).transferir(any(), any(), any());
    }

    private ProcesarOperacionSolicitadaService service() {
        return new ProcesarOperacionSolicitadaService(aplicarOperacionCuentaPort);
    }

    private ProcesarOperacionSolicitadaCommand operacion(
            String tipoOperacion,
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            String importe
    ) {
        return new ProcesarOperacionSolicitadaCommand(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-03T10:15:30Z"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                tipoOperacion,
                cuentaOrigenId,
                cuentaDestinoId,
                new BigDecimal(importe),
                "EUR"
        );
    }
}
