package com.novabank.cuenta.service;

import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.Movimiento;
import com.novabank.cuenta.model.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MovimientoFactory {

    public Movimiento crearDeposito(Cuenta cuenta, BigDecimal cantidad) {
        return crear(cuenta, TipoMovimiento.DEPOSITO, cantidad);
    }

    public Movimiento crearRetiro(Cuenta cuenta, BigDecimal cantidad) {
        return crear(cuenta, TipoMovimiento.RETIRO, cantidad);
    }

    public Movimiento crearTransferenciaSaliente(Cuenta cuenta, BigDecimal cantidad) {
        return crear(cuenta, TipoMovimiento.TRANSFERENCIA_SALIENTE, cantidad);
    }

    public Movimiento crearTransferenciaEntrante(Cuenta cuenta, BigDecimal cantidad) {
        return crear(cuenta, TipoMovimiento.TRANSFERENCIA_ENTRANTE, cantidad);
    }

    private Movimiento crear(Cuenta cuenta, TipoMovimiento tipo, BigDecimal cantidad) {
        Movimiento movimiento = new Movimiento();
        movimiento.setCuenta(cuenta);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        return movimiento;
    }
}
