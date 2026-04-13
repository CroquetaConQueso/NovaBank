package com.novabank.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una operación financiera realizada sobre una cuenta.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Movimiento {

    private static Long idMovimientos = 0L;

    private Long idMovimiento;
    private Cuenta cuentaAsignada;
    private TipoMovimiento tipoMov;
    private BigDecimal cantidadMovimiento;
    private LocalDateTime fechaCreacionMov;

    /**
     * Construye un movimiento asociado a una cuenta.
     *
     * @param cuentaAsignada cuenta afectada por la operación
     * @param tipoMov tipo de movimiento realizado
     * @param cantidadMovimiento cantidad operada
     * @param fechaCreacionMov fecha del movimiento
     */
    @Builder(toBuilder = true)
    public Movimiento(Cuenta cuentaAsignada,
                      TipoMovimiento tipoMov,
                      BigDecimal cantidadMovimiento,
                      LocalDateTime fechaCreacionMov) {
        this.idMovimiento = ++idMovimientos;
        this.cuentaAsignada = cuentaAsignada;
        this.tipoMov = tipoMov;
        this.cantidadMovimiento = cantidadMovimiento;
        this.fechaCreacionMov = fechaCreacionMov;
    }
}