package com.novabank.cuenta.event;

import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.service.CuentaService;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaOperacionEventProcessorTest {

    @Mock
    private CuentaService cuentaService;

    @Mock
    private OperacionResultadoEventPublisher publisher;

    @Test
    void procesarDepositoValidoPublicaOperacionCompletada() {
        CuentaOperacionEventProcessor processor = new CuentaOperacionEventProcessor(cuentaService, publisher);
        OperacionSolicitadaEvent event = operacion("DEPOSITO", null, 10L, "25.00");
        CuentaResponseDTO cuenta = new CuentaResponseDTO(10L, "ES00000000000000000010", 1L, new BigDecimal("125.00"), LocalDateTime.now());

        when(cuentaService.depositar(eq(10L), any(CuentaOperacionRequestDTO.class))).thenReturn(Mono.just(cuenta));
        when(publisher.publicarCompletada(event)).thenReturn(Mono.empty());

        StepVerifier.create(processor.procesar(MessageBuilder.withPayload(event).build()))
                .verifyComplete();

        ArgumentCaptor<CuentaOperacionRequestDTO> requestCaptor = ArgumentCaptor.forClass(CuentaOperacionRequestDTO.class);
        verify(cuentaService).depositar(eq(10L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().cantidad()).isEqualByComparingTo("25.00");
        verify(publisher).publicarCompletada(event);
        verify(publisher, never()).publicarFallida(eq(event), any(), any());
    }

    @Test
    void procesarRetiradaConSaldoInsuficientePublicaOperacionFallida() {
        CuentaOperacionEventProcessor processor = new CuentaOperacionEventProcessor(cuentaService, publisher);
        OperacionSolicitadaEvent event = operacion("RETIRADA", 10L, null, "999.00");

        when(cuentaService.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.error(new InsufficientBalanceException("Saldo insuficiente")));
        when(publisher.publicarFallida(eq(event), eq("SALDO_INSUFICIENTE"), startsWith("Saldo insuficiente")))
                .thenReturn(Mono.empty());

        StepVerifier.create(processor.procesar(MessageBuilder.withPayload(event).build()))
                .verifyComplete();

        ArgumentCaptor<CuentaOperacionRequestDTO> requestCaptor = ArgumentCaptor.forClass(CuentaOperacionRequestDTO.class);
        verify(cuentaService).retirar(eq(10L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().cantidad()).isEqualByComparingTo("999.00");
        verify(publisher).publicarFallida(eq(event), eq("SALDO_INSUFICIENTE"), startsWith("Saldo insuficiente"));
        verify(publisher, never()).publicarCompletada(event);
    }

    @Test
    void procesarDepositoConCuentaInexistentePublicaOperacionFallida() {
        CuentaOperacionEventProcessor processor = new CuentaOperacionEventProcessor(cuentaService, publisher);
        OperacionSolicitadaEvent event = operacion("DEPOSITO", null, 99L, "25.00");

        when(cuentaService.depositar(eq(99L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.error(new ResourceNotFoundException("No existe ninguna cuenta con el id 99")));
        when(publisher.publicarFallida(eq(event), eq("CUENTA_NO_ENCONTRADA"), startsWith("No existe ninguna cuenta")))
                .thenReturn(Mono.empty());

        StepVerifier.create(processor.procesar(MessageBuilder.withPayload(event).build()))
                .verifyComplete();

        verify(cuentaService).depositar(eq(99L), any(CuentaOperacionRequestDTO.class));
        verify(publisher).publicarFallida(eq(event), eq("CUENTA_NO_ENCONTRADA"), startsWith("No existe ninguna cuenta"));
        verify(publisher, never()).publicarCompletada(event);
    }

    private OperacionSolicitadaEvent operacion(
            String tipoOperacion,
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            String importe
    ) {
        return new OperacionSolicitadaEvent(
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
