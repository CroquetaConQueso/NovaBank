package com.novabank.notificacion.service;

import com.novabank.events.alerta.AlertaOperacionSospechosaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OperacionSospechosaNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OperacionSospechosaNotificationService.class);

    public void registrarAlerta(AlertaOperacionSospechosaEvent event) {
        log.warn(
                "Alerta de operacion sospechosa recibida cuentaId={} numeroOperaciones={} ventanaMinutos={} descripcion={} occurredAt={}",
                event.cuentaId(),
                event.numeroOperaciones(),
                event.ventanaMinutos(),
                event.descripcion(),
                event.occurredAt()
        );
    }
}
