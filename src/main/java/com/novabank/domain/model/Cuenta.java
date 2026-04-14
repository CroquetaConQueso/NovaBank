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
 * Representa una cuenta bancaria asociada a un cliente.
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
public class Cuenta {

    private long idCuenta;
    private Cliente dueñoCuenta;
    private String numeroCuenta;
    private BigDecimal saldoCuenta;
    private LocalDateTime fechaCreacionCuenta;
}