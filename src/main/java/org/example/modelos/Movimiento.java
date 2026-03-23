package org.example.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Movimiento {

    private Long idMovimiento;
    private Cuenta cuentaAsignada;
    private TipoMovimiento tipoMov;
    private BigDecimal cantidadMovimiento;
    private LocalDateTime fechaCreacionMov;
}
