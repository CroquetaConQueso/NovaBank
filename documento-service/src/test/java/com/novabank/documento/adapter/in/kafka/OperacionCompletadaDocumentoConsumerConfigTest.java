package com.novabank.documento.adapter.in.kafka;

import com.novabank.documento.application.port.in.GenerarJustificanteOperacionCommand;
import com.novabank.documento.application.port.in.GenerarJustificanteOperacionUseCase;
import com.novabank.documento.domain.model.DocumentoId;
import com.novabank.documento.domain.model.DocumentoOperacion;
import com.novabank.documento.domain.model.TipoDocumento;
import com.novabank.events.operacion.OperacionCompletadaEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.messaging.support.MessageBuilder.withPayload;

class OperacionCompletadaDocumentoConsumerConfigTest {

    @Test
    void convierteEventoCompletadoAComandoDeAplicacion() {
        GenerarJustificanteOperacionUseCase useCase = mock(GenerarJustificanteOperacionUseCase.class);
        when(useCase.generar(any())).thenReturn(Mono.just(new DocumentoOperacion(
                DocumentoId.nuevo(),
                UUID.randomUUID(),
                10L,
                "cuentas/10/operaciones/2026/07/documento.json",
                TipoDocumento.JUSTIFICANTE_OPERACION,
                "application/json",
                Instant.now()
        )));
        OperacionCompletadaDocumentoConsumerConfig config = new OperacionCompletadaDocumentoConsumerConfig();
        UUID operationId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        config.generarJustificanteOperacion(useCase).accept(Flux.just(withPayload(new OperacionCompletadaEvent(
                UUID.randomUUID(),
                correlationId,
                Instant.parse("2026-07-08T08:00:00Z"),
                operationId,
                "DEPOSITO",
                null,
                10L,
                10L,
                99L,
                new BigDecimal("25.00"),
                "EUR"
        )).build()));

        ArgumentCaptor<GenerarJustificanteOperacionCommand> captor =
                ArgumentCaptor.forClass(GenerarJustificanteOperacionCommand.class);
        verify(useCase).generar(captor.capture());
        assertThat(captor.getValue().operationId()).isEqualTo(operationId);
        assertThat(captor.getValue().correlationId()).isEqualTo(correlationId);
        assertThat(captor.getValue().tipoOperacion()).isEqualTo("DEPOSITO");
        assertThat(captor.getValue().cuentaOrigenId()).isNull();
        assertThat(captor.getValue().cuentaDestinoId()).isEqualTo(10L);
        assertThat(captor.getValue().cuentaId()).isEqualTo(10L);
        assertThat(captor.getValue().movimientoId()).isEqualTo(99L);
    }

    @Test
    void noInvocaUseCaseSiElEventoNoTraeCuentaPrincipal() {
        GenerarJustificanteOperacionUseCase useCase = mock(GenerarJustificanteOperacionUseCase.class);
        OperacionCompletadaDocumentoConsumerConfig config = new OperacionCompletadaDocumentoConsumerConfig();

        config.generarJustificanteOperacion(useCase).accept(Flux.just(withPayload(new OperacionCompletadaEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-07-08T08:00:00Z"),
                UUID.randomUUID(),
                "DEPOSITO",
                null,
                10L,
                null,
                99L,
                new BigDecimal("25.00"),
                "EUR"
        )).build()));

        verify(useCase, never()).generar(any());
    }
}
