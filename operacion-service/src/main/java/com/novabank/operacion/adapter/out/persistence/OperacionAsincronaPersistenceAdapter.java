package com.novabank.operacion.adapter.out.persistence;

import com.novabank.operacion.application.port.out.OperacionAsincronaRepositoryPort;
import com.novabank.operacion.domain.model.EstadoOperacionAsincrona;
import com.novabank.operacion.repository.OperacionAsincronaRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class OperacionAsincronaPersistenceAdapter implements OperacionAsincronaRepositoryPort {

    private final OperacionAsincronaRepository repository;

    public OperacionAsincronaPersistenceAdapter(OperacionAsincronaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<com.novabank.operacion.domain.model.OperacionAsincrona> save(
            com.novabank.operacion.domain.model.OperacionAsincrona operacion
    ) {
        return repository.save(toEntity(operacion))
                .map(this::toDomain);
    }

    @Override
    public Mono<com.novabank.operacion.domain.model.OperacionAsincrona> findByOperationId(UUID operationId) {
        return repository.findById(operationId)
                .map(this::toDomain);
    }

    private com.novabank.operacion.model.OperacionAsincrona toEntity(
            com.novabank.operacion.domain.model.OperacionAsincrona operacion
    ) {
        com.novabank.operacion.model.OperacionAsincrona entity =
                com.novabank.operacion.model.OperacionAsincrona.builder()
                        .operationId(operacion.operationId())
                        .correlationId(operacion.correlationId())
                        .tipoOperacion(operacion.tipoOperacion())
                        .cuentaId(operacion.cuentaId())
                        .cuentaOrigenId(operacion.cuentaOrigenId())
                        .cuentaDestinoId(operacion.cuentaDestinoId())
                        .importe(operacion.importe())
                        .moneda(operacion.moneda())
                        .estado(com.novabank.operacion.model.EstadoOperacionAsincrona.valueOf(operacion.estado().name()))
                        .motivoFallo(operacion.motivoFallo())
                        .creadaEn(operacion.creadaEn())
                        .actualizadaEn(operacion.actualizadaEn())
                        .build();
        entity.setNueva(operacion.creadaEn() != null
                && operacion.actualizadaEn() != null
                && operacion.creadaEn().equals(operacion.actualizadaEn()));
        return entity;
    }

    private com.novabank.operacion.domain.model.OperacionAsincrona toDomain(
            com.novabank.operacion.model.OperacionAsincrona entity
    ) {
        return new com.novabank.operacion.domain.model.OperacionAsincrona(
                entity.getOperationId(),
                entity.getCorrelationId(),
                entity.getTipoOperacion(),
                entity.getCuentaId(),
                entity.getCuentaOrigenId(),
                entity.getCuentaDestinoId(),
                entity.getImporte(),
                entity.getMoneda(),
                EstadoOperacionAsincrona.valueOf(entity.getEstado().name()),
                entity.getMotivoFallo(),
                entity.getCreadaEn(),
                entity.getActualizadaEn()
        );
    }
}
