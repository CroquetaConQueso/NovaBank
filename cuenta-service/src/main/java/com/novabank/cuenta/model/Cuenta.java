package com.novabank.cuenta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("cuentas")
public class Cuenta {

    @Id
    private Long id;

    @Column("numero_cuenta")
    private String numeroCuenta;

    @Column("cliente_id")
    private Long clienteId;

    @Column("saldo")
    private BigDecimal saldo;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    /**
     * Spring Data R2DBC usa este campo para detectar escrituras concurrentes
     * sobre una misma cuenta.
     */
    @Version
    @Column("version")
    private Long version;

    public void prepararParaCreacion() {
        if (saldo == null) {
            saldo = BigDecimal.ZERO;
        }
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
