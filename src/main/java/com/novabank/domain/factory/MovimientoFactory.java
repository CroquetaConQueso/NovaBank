package com.novabank.domain.factory;

import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Factoría de movimientos.
 *
 * Centraliza la creación de objetos Movimiento para evitar duplicar
 * la lógica de construcción en los servicios.
 */
public final class MovimientoFactory {

    private MovimientoFactory() {
    }

    public static Movimiento crearDeposito(Cuenta cuenta, BigDecimal monto) {
        return crear(cuenta, TipoMovimiento.DEPOSITO, monto);
    }

    public static Movimiento crearRetiro(Cuenta cuenta, BigDecimal monto) {
        return crear(cuenta, TipoMovimiento.RETIRO, monto);
    }

    public static Movimiento crearTransferenciaSaliente(Cuenta cuenta, BigDecimal monto) {
        return crear(cuenta, TipoMovimiento.TRANSFERENCIA_SALIENTE, monto);
    }

    public static Movimiento crearTransferenciaEntrante(Cuenta cuenta, BigDecimal monto) {
        return crear(cuenta, TipoMovimiento.TRANSFERENCIA_ENTRANTE, monto);
    }

    private static Movimiento crear(Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal monto) {
        return Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(tipoMovimiento)
                .cantidadMovimiento(monto)
                .fechaCreacionMov(LocalDateTime.now())
                .build();
    }
}