package org.example.modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Cuenta {

    private static long idCuentas = 0L;

    private long idCuenta;
    private Cliente dueñoCuenta;
    private String numeroCuenta;
    private BigDecimal saldoCuenta;
    private LocalDateTime fechaCreacionCuenta;

    /**
     * Representa una cuenta bancaria asociada a un cliente.
     *
     * Gestiona el número de cuenta, el saldo actual
     * y la fecha de creación y tiene su id autogenerado
     */
    public Cuenta(Cliente cli, String numeroCu, BigDecimal saldoCu, LocalDateTime fechaCre) {
        this.idCuenta = ++idCuentas;
        this.dueñoCuenta = cli;
        this.numeroCuenta = numeroCu;
        this.saldoCuenta = saldoCu;
        this.fechaCreacionCuenta = fechaCre;
    }

    public Cliente getDueñoCuenta() {
        return dueñoCuenta;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public BigDecimal getSaldoCuenta() {
        return saldoCuenta;
    }

    public LocalDateTime getFechaCreacionCuenta() {
        return fechaCreacionCuenta;
    }


    public void setSaldoCuenta(BigDecimal saldoCuenta) {
        this.saldoCuenta = saldoCuenta;
    }

}
