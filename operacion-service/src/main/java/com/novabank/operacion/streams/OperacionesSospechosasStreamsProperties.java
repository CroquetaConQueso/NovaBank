package com.novabank.operacion.streams;

import com.novabank.events.core.NovaBankTopics;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "novabank.kafka-streams.operaciones-sospechosas")
public class OperacionesSospechosasStreamsProperties {

    private boolean enabled = true;
    private String applicationId = "operacion-service-suspicious-streams";
    private String bootstrapServers = "localhost:9092";
    private String inputTopic = NovaBankTopics.OPERACIONES_SOLICITADAS;
    private String outputTopic = NovaBankTopics.ALERTAS_OPERACIONES_SOSPECHOSAS;
    private long ventanaMinutos = 10;
    private long umbralOperaciones = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getInputTopic() {
        return inputTopic;
    }

    public void setInputTopic(String inputTopic) {
        this.inputTopic = inputTopic;
    }

    public String getOutputTopic() {
        return outputTopic;
    }

    public void setOutputTopic(String outputTopic) {
        this.outputTopic = outputTopic;
    }

    public long getVentanaMinutos() {
        return ventanaMinutos;
    }

    public void setVentanaMinutos(long ventanaMinutos) {
        this.ventanaMinutos = ventanaMinutos;
    }

    public long getUmbralOperaciones() {
        return umbralOperaciones;
    }

    public void setUmbralOperaciones(long umbralOperaciones) {
        this.umbralOperaciones = umbralOperaciones;
    }
}
