package com.novabank.documento.adapter.in.kafka;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OperacionCompletadaDocumentoConsumerConfig {

    /**
     * Esqueleto controlado para la siguiente iteracion del Modulo 7.
     *
     * <p>El servicio debera reaccionar a {@link OperacionCompletadaEvent} para generar
     * justificantes, pero esta rama no registra ningun bean Consumer ni integra S3/AWS.
     * Asi se evita activar suscripciones Kafka antes de implementar el caso completo.</p>
     */
    public void documentarEventoFuturo(OperacionCompletadaEvent event) {
        // TODO Modulo 7: convertir OperacionCompletadaEvent a comando de aplicacion.
    }
}
