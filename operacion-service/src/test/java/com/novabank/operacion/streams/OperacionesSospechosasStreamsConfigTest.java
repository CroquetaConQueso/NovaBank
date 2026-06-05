package com.novabank.operacion.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.events.alerta.AlertaOperacionSospechosaEvent;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import com.novabank.operacion.streams.serde.JacksonJsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OperacionesSospechosasStreamsConfigTest {

    private static final String INPUT_TOPIC = "test.operaciones.solicitadas";
    private static final String OUTPUT_TOPIC = "test.alertas.operaciones-sospechosas";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private TopologyTestDriver driver;
    private TestInputTopic<String, OperacionSolicitadaEvent> input;
    private TestOutputTopic<String, AlertaOperacionSospechosaEvent> output;

    @BeforeEach
    void setUp() {
        OperacionesSospechosasStreamsProperties properties = new OperacionesSospechosasStreamsProperties();
        properties.setInputTopic(INPUT_TOPIC);
        properties.setOutputTopic(OUTPUT_TOPIC);
        properties.setVentanaMinutos(10);
        properties.setUmbralOperaciones(5);

        Topology topology = new OperacionesSospechosasStreamsConfig()
                .operacionesSospechosasTopology(objectMapper, properties);

        Properties streamsProperties = new Properties();
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-operaciones-sospechosas");
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        streamsProperties.put(StreamsConfig.STATE_DIR_CONFIG, "target/kafka-streams-test-" + UUID.randomUUID());

        Serde<OperacionSolicitadaEvent> operacionSerde =
                new JacksonJsonSerde<>(objectMapper, OperacionSolicitadaEvent.class);
        Serde<AlertaOperacionSospechosaEvent> alertaSerde =
                new JacksonJsonSerde<>(objectMapper, AlertaOperacionSospechosaEvent.class);

        driver = new TopologyTestDriver(topology, streamsProperties);
        input = driver.createInputTopic(
                INPUT_TOPIC,
                Serdes.String().serializer(),
                operacionSerde.serializer(),
                Instant.parse("2026-06-04T10:00:00Z"),
                Duration.ofMinutes(1)
        );
        output = driver.createOutputTopic(
                OUTPUT_TOPIC,
                Serdes.String().deserializer(),
                alertaSerde.deserializer()
        );
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.close();
        }
    }

    @Test
    void cincoRetiradasEnDiezMinutosNoGeneranAlerta() {
        for (int i = 0; i < 5; i++) {
            input.pipeInput("10", solicitud("RETIRADA", 10L));
        }

        assertThat(output.isEmpty()).isTrue();
    }

    @Test
    void seisRetiradasEnDiezMinutosGeneranAlerta() {
        for (int i = 0; i < 6; i++) {
            input.pipeInput("10", solicitud("RETIRADA", 10L));
        }

        assertThat(output.isEmpty()).isFalse();
        AlertaOperacionSospechosaEvent event = output.readValue();
        assertThat(event.cuentaId()).isEqualTo(10L);
        assertThat(event.numeroOperaciones()).isEqualTo(6L);
        assertThat(event.ventanaMinutos()).isEqualTo(10L);
        assertThat(event.tipoOperacion()).isEqualTo("RETIRADA");
        assertThat(event.descripcion()).contains("Mas de 5 retiradas");
        assertThat(output.isEmpty()).isTrue();
    }

    @Test
    void retiradasDeCuentasDistintasNoSeMezclan() {
        for (int i = 0; i < 5; i++) {
            input.pipeInput("10", solicitud("RETIRADA", 10L));
            input.pipeInput("11", solicitud("RETIRADA", 11L));
        }

        assertThat(output.isEmpty()).isTrue();
    }

    @Test
    void depositosNoCuentanComoRetiradasSospechosas() {
        for (int i = 0; i < 6; i++) {
            input.pipeInput("10", solicitud("DEPOSITO", 10L));
        }

        assertThat(output.isEmpty()).isTrue();
    }

    @Test
    void eventosSinCuentaOrigenSeIgnoran() {
        for (int i = 0; i < 6; i++) {
            input.pipeInput("10", solicitud("RETIRADA", null));
        }

        assertThat(output.isEmpty()).isTrue();
    }

    private OperacionSolicitadaEvent solicitud(String tipoOperacion, Long cuentaOrigenId) {
        return new OperacionSolicitadaEvent(
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-04T10:00:00Z"),
                UUID.randomUUID(),
                tipoOperacion,
                cuentaOrigenId,
                "DEPOSITO".equals(tipoOperacion) ? 10L : null,
                new BigDecimal("10.00"),
                "EUR"
        );
    }
}
