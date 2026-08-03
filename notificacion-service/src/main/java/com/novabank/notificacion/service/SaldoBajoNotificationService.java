package com.novabank.notificacion.service;

import com.novabank.events.alerta.AlertaSaldoBajoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SaldoBajoNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SaldoBajoNotificationService.class);

    public void registrarAlerta(AlertaSaldoBajoEvent event) {
        log.warn(
                "Alerta de saldo bajo preparada cuentaId={} saldoActual={} umbral={} moneda={} correlationId={}",
                event.cuentaId(),
                event.saldoActual(),
                event.umbral(),
                event.moneda(),
                event.correlationId()
        );
    }
}
