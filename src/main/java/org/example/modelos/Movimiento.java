package org.example.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Movimiento {

    private static Long idMovimientos = 0L;
    private Long idMovimiento;
    private Cuenta cuentaAsignada;
    private TipoMovimiento tipoMov;
    private BigDecimal cantidadMovimiento;
    private LocalDateTime fechaCreacionMov;

    public Movimiento(Cuenta cuentaAsignada, TipoMovimiento tipoMov, BigDecimal cantidadMovimiento, LocalDateTime fechaCreacionMov) {
        this.idMovimiento = ++idMovimientos;
        this.cuentaAsignada = cuentaAsignada;
        this.tipoMov = tipoMov;
        this.cantidadMovimiento = cantidadMovimiento;
        this.fechaCreacionMov = fechaCreacionMov;
    }
}
