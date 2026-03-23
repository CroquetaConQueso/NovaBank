package org.example.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Cuenta {
    private static long idCuentas = 0L;
    private long idCuenta;
    private Cliente dueñoCuenta;
    private String numeroCuenta;
    private BigDecimal saldoCuenta;
    private LocalDateTime fechaCreacionCuenta;

    public Cuenta(Cliente cli, String numeroCu, BigDecimal saldoCu,LocalDateTime fechaCre ){
        this.idCuenta = ++idCuentas;
        this.dueñoCuenta = cli;
        this.numeroCuenta = numeroCu;
        this.saldoCuenta = saldoCu;
        this.fechaCreacionCuenta = fechaCre;
    }
}
