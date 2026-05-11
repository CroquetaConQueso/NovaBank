package com.novabank.cuenta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("operaciones_idempotentes")
public class OperacionIdempotente {

    @Id
    private Long id;

    @Column("operation_id")
    private String operationId;

    @Column("request_hash")
    private String requestHash;

    @Column("estado")
    private EstadoOperacionIdempotente estado;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public void prepararParaCreacion() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
    }

    public void marcarCompletada() {
        estado = EstadoOperacionIdempotente.COMPLETED;
        fechaActualizacion = LocalDateTime.now();
    }
}
