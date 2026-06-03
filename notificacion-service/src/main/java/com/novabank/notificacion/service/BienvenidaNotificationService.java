package com.novabank.notificacion.service;

import com.novabank.events.cliente.ClienteRegistradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BienvenidaNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BienvenidaNotificationService.class);

    public void registrarBienvenida(ClienteRegistradoEvent event) {
        log.info(
                "Notificacion de bienvenida preparada para clienteId={}, nombre={}, email={}",
                event.clienteId(),
                event.nombre(),
                event.email()
        );
    }
}
