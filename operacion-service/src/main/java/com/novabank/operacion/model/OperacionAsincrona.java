package com.novabank.operacion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("operaciones_asincronas")
public class OperacionAsincrona implements Persistable<UUID> {

    @Id
    @Column("operation_id")
    private UUID operationId;

    @Column("correlation_id")
    private UUID correlationId;

    @Column("tipo_operacion")
    private String tipoOperacion;

    @Column("cuenta_id")
    private Long cuentaId;

    @Column("cuenta_origen_id")
    private Long cuentaOrigenId;

    @Column("cuenta_destino_id")
    private Long cuentaDestinoId;

    @Column("importe")
    private BigDecimal importe;

    @Column("moneda")
    private String moneda;

    @Column("estado")
    private EstadoOperacionAsincrona estado;

    @Column("motivo_fallo")
    private String motivoFallo;

    @Column("creada_en")
    private LocalDateTime creadaEn;

    @Column("actualizada_en")
    private LocalDateTime actualizadaEn;

    @Transient
    private boolean nueva;

    @Override
    public UUID getId() {
        return operationId;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }

    public void prepararParaCreacion() {
        LocalDateTime ahora = LocalDateTime.now();
        if (creadaEn == null) {
            creadaEn = ahora;
        }
        actualizadaEn = ahora;
        estado = EstadoOperacionAsincrona.SOLICITADA;
        nueva = true;
    }

    public void marcarCompletada() {
        estado = EstadoOperacionAsincrona.COMPLETADA;
        motivoFallo = null;
        actualizadaEn = LocalDateTime.now();
    }

    public void marcarFallida(String motivo) {
        estado = EstadoOperacionAsincrona.FALLIDA;
        motivoFallo = motivo;
        actualizadaEn = LocalDateTime.now();
    }

    public boolean estaCompletada() {
        return estado == EstadoOperacionAsincrona.COMPLETADA;
    }

    public boolean estaFallida() {
        return estado == EstadoOperacionAsincrona.FALLIDA;
    }
}
