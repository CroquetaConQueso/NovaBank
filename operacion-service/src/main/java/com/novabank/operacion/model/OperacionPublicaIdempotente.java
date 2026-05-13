package com.novabank.operacion.model;

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
@Table("operaciones_publicas_idempotentes")
public class OperacionPublicaIdempotente {

    @Id
    private Long id;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("request_hash")
    private String requestHash;

    @Column("tipo_operacion")
    private String tipoOperacion;

    @Column("estado")
    private EstadoOperacionPublicaIdempotente estado;

    @Column("response_json")
    private String responseJson;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public void marcarCompletada(String responseJson) {
        this.estado = EstadoOperacionPublicaIdempotente.COMPLETED;
        this.responseJson = responseJson;
        this.fechaActualizacion = LocalDateTime.now();
    }
}
