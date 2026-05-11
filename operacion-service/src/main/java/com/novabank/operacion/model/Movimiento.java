package com.novabank.operacion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("movimientos")
public class Movimiento {

    @Id
    private Long id;

    @Column("cuenta_id")
    private Long cuentaId;

    @Column("numero_cuenta")
    private String numeroCuenta;

    @Column("tipo")
    private TipoMovimiento tipo;

    @Column("cantidad")
    private BigDecimal cantidad;

    @Column("fecha")
    private LocalDateTime fecha;

    public void prepararParaCreacion() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
