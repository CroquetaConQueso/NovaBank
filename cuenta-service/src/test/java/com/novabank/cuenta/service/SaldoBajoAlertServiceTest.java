package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.event.AlertaSaldoBajoEventPublisher;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaldoBajoAlertServiceTest {

    private final AlertaSaldoBajoEventPublisher publisher = mock(AlertaSaldoBajoEventPublisher.class);
    private final SaldoBajoAlertService service = new SaldoBajoAlertService(publisher, new BigDecimal("100.00"));

    @Test
    void saldoMenorQueUmbralPublicaAlerta() {
        MovimientoEventDTO evento = evento("99.99");
        when(publisher.publicar(evento, new BigDecimal("100.00"))).thenReturn(Mono.empty());

        StepVerifier.create(service.evaluarYPublicar(evento))
                .verifyComplete();

        verify(publisher).publicar(evento, new BigDecimal("100.00"));
    }

    @Test
    void saldoIgualAlUmbralNoPublicaAlerta() {
        MovimientoEventDTO evento = evento("100.00");

        StepVerifier.create(service.evaluarYPublicar(evento))
                .verifyComplete();

        verify(publisher, never()).publicar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void saldoMayorQueUmbralNoPublicaAlerta() {
        MovimientoEventDTO evento = evento("100.01");

        StepVerifier.create(service.evaluarYPublicar(evento))
                .verifyComplete();

        verify(publisher, never()).publicar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void eventoSinSaldoNoPublicaAlerta() {
        MovimientoEventDTO evento = new MovimientoEventDTO(
                10L,
                null,
                "RETIRO",
                new BigDecimal("25.00"),
                null,
                "Retiro interno",
                LocalDateTime.now(),
                "op-test"
        );

        StepVerifier.create(service.evaluarYPublicar(evento))
                .verifyComplete();

        verify(publisher, never()).publicar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MovimientoEventDTO evento(String saldoResultante) {
        return new MovimientoEventDTO(
                10L,
                null,
                "RETIRO",
                new BigDecimal("25.00"),
                new BigDecimal(saldoResultante),
                "Retiro interno",
                LocalDateTime.now(),
                "op-test"
        );
    }
}
