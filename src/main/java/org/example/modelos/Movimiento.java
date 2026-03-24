package org.example.modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimiento {

    private static Long idMovimientos = 0L;

    private Long idMovimiento;
    private Cuenta cuentaAsignada;
    private TipoMovimiento tipoMov;
    private BigDecimal cantidadMovimiento;
    private LocalDateTime fechaCreacionMov;

    public Movimiento(Cuenta cuentaAsignada, TipoMovimiento tipoMov,
                      BigDecimal cantidadMovimiento, LocalDateTime fechaCreacionMov) {
        this.idMovimiento = ++idMovimientos;
        this.cuentaAsignada = cuentaAsignada;
        this.tipoMov = tipoMov;
        this.cantidadMovimiento = cantidadMovimiento;
        this.fechaCreacionMov = fechaCreacionMov;
    }

    // Getters

    public Long getIdMovimiento() {
        return idMovimiento;
    }

    public Cuenta getCuentaAsignada() {
        return cuentaAsignada;
    }

    public TipoMovimiento getTipoMov() {
        return tipoMov;
    }

    public BigDecimal getCantidadMovimiento() {
        return cantidadMovimiento;
    }

    public LocalDateTime getFechaCreacionMov() {
        return fechaCreacionMov;
    }

}
