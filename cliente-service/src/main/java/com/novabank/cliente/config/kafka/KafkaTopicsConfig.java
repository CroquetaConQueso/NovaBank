package com.novabank.cliente.config.kafka;

import com.novabank.events.core.NovaBankTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    private static final int REPLICAS_LOCAL = 1;
    private static final String RETENTION_MS_CONFIG = "retention.ms";
    private static final String RETENTION_7_DAYS_MS = "604800000";
    private static final String RETENTION_30_DAYS_MS = "2592000000";

    @Bean
    public NewTopic clientesRegistradosTopic() {
        return topic(NovaBankTopics.CLIENTES_REGISTRADOS, 3, RETENTION_7_DAYS_MS);
    }

    @Bean
    public NewTopic operacionesSolicitadasTopic() {
        return topic(NovaBankTopics.OPERACIONES_SOLICITADAS, 6, RETENTION_7_DAYS_MS);
    }

    @Bean
    public NewTopic operacionesCompletadasTopic() {
        return topic(NovaBankTopics.OPERACIONES_COMPLETADAS, 6, RETENTION_7_DAYS_MS);
    }

    @Bean
    public NewTopic operacionesFallidasTopic() {
        return topic(NovaBankTopics.OPERACIONES_FALLIDAS, 6, RETENTION_30_DAYS_MS);
    }

    @Bean
    public NewTopic movimientosRegistradosTopic() {
        return topic(NovaBankTopics.MOVIMIENTOS_REGISTRADOS, 6, RETENTION_7_DAYS_MS);
    }

    @Bean
    public NewTopic alertasSaldoBajoTopic() {
        return topic(NovaBankTopics.ALERTAS_SALDO_BAJO, 3, RETENTION_30_DAYS_MS);
    }

    @Bean
    public NewTopic alertasOperacionesSospechosasTopic() {
        return topic(NovaBankTopics.ALERTAS_OPERACIONES_SOSPECHOSAS, 3, RETENTION_30_DAYS_MS);
    }

    private NewTopic topic(String name, int partitions, String retentionMs) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(REPLICAS_LOCAL)
                .config(RETENTION_MS_CONFIG, retentionMs)
                .build();
    }
}
