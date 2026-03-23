package org.example.modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimiento {

    private Long idMovimiento;
    private Cuenta cuentaAsignada;
    private TipoMovimiento tipoMov;
    private BigDecimal cantidadMovimiento;
    private LocalDateTime fechaCreacionMov;
}
