package com.novabank.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una operación financiera realizada sobre una cuenta.
 *
 * En persistencia JDBC, el identificador lo genera PostgreSQL.
 * La entidad no debe autogenerar IDs por su cuenta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Movimiento {

    private Long idMovimiento;
    private Cuenta cuentaAsignada;
    private TipoMovimiento tipoMov;
    private BigDecimal cantidadMovimiento;
    private LocalDateTime fechaCreacionMov;
}