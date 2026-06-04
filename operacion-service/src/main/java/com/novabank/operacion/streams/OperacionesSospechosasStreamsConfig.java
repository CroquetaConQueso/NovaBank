package com.novabank.operacion.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.events.alerta.AlertaOperacionSospechosaEvent;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import com.novabank.operacion.streams.serde.JacksonJsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(OperacionesSospechosasStreamsProperties.class)
public class OperacionesSospechosasStreamsConfig {

    private static final Logger log = LoggerFactory.getLogger(OperacionesSospechosasStreamsConfig.class);

    @Bean
    public Topology operacionesSospechosasTopology(
            ObjectMapper objectMapper,
            OperacionesSospechosasStreamsProperties properties
    ) {
        StreamsBuilder builder = new StreamsBuilder();
        Serde<String> stringSerde = Serdes.String();
        Serde<OperacionSolicitadaEvent> operacionSerde =
                new JacksonJsonSerde<>(objectMapper, OperacionSolicitadaEvent.class);
        Serde<RetiradaWindowAggregate> aggregateSerde =
                new JacksonJsonSerde<>(objectMapper, RetiradaWindowAggregate.class);
        Serde<AlertaOperacionSospechosaEvent> alertaSerde =
                new JacksonJsonSerde<>(objectMapper, AlertaOperacionSospechosaEvent.class);

        builder.stream(properties.getInputTopic(), Consumed.with(stringSerde, operacionSerde))
                .filter((key, event) -> esRetiradaConCuentaOrigen(event))
                .peek((key, event) -> log.info(
                        "Operacion de retirada detectada cuentaId={} tipoOperacion={} operationId={}",
                        event.cuentaOrigenId(),
                        event.tipoOperacion(),
                        event.operationId()
                ))
                .selectKey((key, event) -> String.valueOf(event.cuentaOrigenId()))
                .groupByKey(Grouped.with(stringSerde, operacionSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(properties.getVentanaMinutos())))
                .aggregate(
                        RetiradaWindowAggregate::empty,
                        (cuentaId, event, aggregate) -> aggregate.incrementar(
                                event.correlationId(),
                                tipoNormalizado(event.tipoOperacion())
                        ),
                        Materialized.<String, RetiradaWindowAggregate>as(
                                        Stores.inMemoryWindowStore(
                                                "operaciones-sospechosas-window-store",
                                                Duration.ofMinutes(properties.getVentanaMinutos() * 2),
                                                Duration.ofMinutes(properties.getVentanaMinutos()),
                                                false
                                        )
                                )
                                .withKeySerde(stringSerde)
                                .withValueSerde(aggregateSerde)
                )
                .toStream()
                .peek((window, aggregate) -> log.info(
                        "Ventana de retiradas evaluada cuentaId={} numeroOperaciones={} ventanaInicio={} ventanaFin={}",
                        window.key(),
                        aggregate.numeroOperaciones(),
                        Instant.ofEpochMilli(window.window().start()),
                        Instant.ofEpochMilli(window.window().end())
                ))
                .filter((window, aggregate) -> aggregate.numeroOperaciones() > properties.getUmbralOperaciones())
                .map((window, aggregate) -> KeyValue.pair(
                        window.key(),
                        alerta(window, aggregate, properties)
                ))
                .peek((cuentaId, alerta) -> log.warn(
                        "Alerta de operacion sospechosa generada cuentaId={} numeroOperaciones={} ventanaMinutos={}",
                        alerta.cuentaId(),
                        alerta.numeroOperaciones(),
                        alerta.ventanaMinutos()
                ))
                .to(properties.getOutputTopic(), Produced.with(stringSerde, alertaSerde));

        return builder.build();
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "novabank.kafka-streams.operaciones-sospechosas",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public KafkaStreams operacionesSospechosasKafkaStreams(
            Topology operacionesSospechosasTopology,
            OperacionesSospechosasStreamsProperties properties
    ) {
        Properties streamsProperties = new Properties();
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, properties.getApplicationId());
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class);
        return new KafkaStreams(operacionesSospechosasTopology, streamsProperties);
    }

    private boolean esRetiradaConCuentaOrigen(OperacionSolicitadaEvent event) {
        return event != null
                && event.cuentaOrigenId() != null
                && ("RETIRADA".equals(tipoNormalizado(event.tipoOperacion()))
                || "RETIRO".equals(tipoNormalizado(event.tipoOperacion())));
    }

    private String tipoNormalizado(String tipoOperacion) {
        return tipoOperacion == null ? "" : tipoOperacion.trim().toUpperCase(Locale.ROOT);
    }

    private AlertaOperacionSospechosaEvent alerta(
            Windowed<String> window,
            RetiradaWindowAggregate aggregate,
            OperacionesSospechosasStreamsProperties properties
    ) {
        Long cuentaId = Long.valueOf(window.key());
        String descripcion = "Mas de " + properties.getUmbralOperaciones()
                + " retiradas en " + properties.getVentanaMinutos() + " minutos";

        return new AlertaOperacionSospechosaEvent(
                UUID.randomUUID(),
                aggregate.correlationId(),
                Instant.now(),
                cuentaId,
                aggregate.tipoOperacion(),
                aggregate.numeroOperaciones(),
                properties.getVentanaMinutos(),
                descripcion
        );
    }
}
