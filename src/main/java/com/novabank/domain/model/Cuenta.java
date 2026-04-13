package com.novabank.domain.model;

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
 * En este issue se conserva la relación actual con Cliente para no mezclar
 * todavía este cambio con el futuro alineamiento a JDBC.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Cuenta {

    private static long idCuentas = 0L;

    private long idCuenta;
    private Cliente dueñoCuenta;
    private String numeroCuenta;
    private BigDecimal saldoCuenta;
    private LocalDateTime fechaCreacionCuenta;

    /**
     * Construye una cuenta bancaria con su titular, número, saldo y fecha de creación.
     *
     * @param dueñoCuenta titular de la cuenta
     * @param numeroCuenta número identificativo de la cuenta
     * @param saldoCuenta saldo actual
     * @param fechaCreacionCuenta fecha de creación
     */
    @Builder(toBuilder = true)
    public Cuenta(Cliente dueñoCuenta,
                  String numeroCuenta,
                  BigDecimal saldoCuenta,
                  LocalDateTime fechaCreacionCuenta) {
        this.idCuenta = ++idCuentas;
        this.dueñoCuenta = dueñoCuenta;
        this.numeroCuenta = numeroCuenta;
        this.saldoCuenta = saldoCuenta;
        this.fechaCreacionCuenta = fechaCreacionCuenta;
    }
}