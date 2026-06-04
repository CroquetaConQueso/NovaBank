package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.event.AlertaSaldoBajoEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class SaldoBajoAlertService {

    private static final Logger log = LoggerFactory.getLogger(SaldoBajoAlertService.class);

    private final AlertaSaldoBajoEventPublisher publisher;
    private final BigDecimal umbral;

    public SaldoBajoAlertService(
            AlertaSaldoBajoEventPublisher publisher,
            @Value("${novabank.alertas.saldo-bajo.umbral:100.00}") BigDecimal umbral
    ) {
        this.publisher = publisher;
        this.umbral = umbral;
    }

    public Mono<Void> evaluarYPublicar(MovimientoEventDTO movimiento) {
        return Mono.defer(() -> {
            BigDecimal saldo = movimiento == null ? null : movimiento.saldoResultante();
            Long cuentaId = movimiento == null ? null : movimiento.cuentaId();

            log.info("saldo evaluado para alerta cuentaId={} saldoActual={} umbral={}", cuentaId, saldo, umbral);

            if (movimiento == null || cuentaId == null || saldo == null || saldo.compareTo(umbral) >= 0) {
                return Mono.empty();
            }

            return publisher.publicar(movimiento, umbral)
                    .doOnSuccess(ignored -> log.info(
                            "alerta de saldo bajo solicitada cuentaId={} saldoActual={} umbral={}",
                            cuentaId,
                            saldo,
                            umbral
                    ));
        });
    }
}
