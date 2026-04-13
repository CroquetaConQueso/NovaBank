package com.novabank.service;

import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.memory.RepositorioCuenta;
import com.novabank.persistence.memory.RepositorioMovimiento;
import com.novabank.util.Utilidades;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de operaciones financieras.
 *
 * Centraliza la lógica de depósitos, retiros, transferencias y consulta
 * de movimientos, manteniendo la persistencia en memoria en esta fase.
 */
public class MovimientoServicio {

    private final RepositorioCuenta repoCuenta;
    private final RepositorioMovimiento repoMovi;

    public MovimientoServicio(RepositorioCuenta repoCuenta, RepositorioMovimiento repoMovi) {
        this.repoCuenta = repoCuenta;
        this.repoMovi = repoMovi;
    }

    /**
     * Aplica un depósito a una cuenta existente y registra el movimiento.
     *
     * @param numeroCuenta número de cuenta destino
     * @param cantidad importe a depositar
     * @return cuenta actualizada
     */
    public Cuenta depositar(String numeroCuenta, BigDecimal cantidad) {
        Cuenta cuenta = obtenerCuentaValida(numeroCuenta);
        validarCantidadPositiva(cantidad, "depositar");

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().add(cantidad));
        registrarMovimiento(cuenta, TipoMovimiento.DEPOSITO, cantidad);

        return cuenta;
    }

    /**
     * Aplica un retiro sobre una cuenta existente si dispone de saldo suficiente.
     *
     * @param numeroCuenta número de cuenta origen
     * @param cantidad importe a retirar
     * @return cuenta actualizada
     */
    public Cuenta retirar(String numeroCuenta, BigDecimal cantidad) {
        Cuenta cuenta = obtenerCuentaValida(numeroCuenta);
        validarCantidadPositiva(cantidad, "retirar");

        if (cuenta.getSaldoCuenta().compareTo(cantidad) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente.\nSaldo disponible: " + cuenta.getSaldoCuenta()
                            + " €\nImporte solicitado: " + cantidad + " €"
            );
        }

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().subtract(cantidad));
        registrarMovimiento(cuenta, TipoMovimiento.RETIRO, cantidad);

        return cuenta;
    }

    /**
     * Transfiere saldo entre dos cuentas distintas, registrando un movimiento
     * de salida y otro de entrada.
     *
     * @param numeroOrigen cuenta origen
     * @param numeroDestino cuenta destino
     * @param cantidad importe a transferir
     */
    public void transferir(String numeroOrigen, String numeroDestino, BigDecimal cantidad) {
        String origenNormalizado = normalizarNumeroCuenta(numeroOrigen);
        String destinoNormalizado = normalizarNumeroCuenta(numeroDestino);

        validarNumeroCuenta(origenNormalizado);
        validarNumeroCuenta(destinoNormalizado);
        validarCantidadPositiva(cantidad, "transferir");

        if (origenNormalizado.equals(destinoNormalizado)) {
            throw new ValidationException("La cuenta origen y destino deben de ser diferentes");
        }

        Cuenta cuentaOrigen = obtenerCuentaValida(origenNormalizado);
        Cuenta cuentaDestino = obtenerCuentaValida(destinoNormalizado);

        if (cuentaOrigen.getSaldoCuenta().compareTo(cantidad) < 0) {
            throw new InsufficientBalanceException(
                    "La cantidad a transferir no puede ser mayor que la cantidad encontrada en la cuenta"
            );
        }

        BigDecimal saldoOrigenAnterior = cuentaOrigen.getSaldoCuenta();
        BigDecimal saldoDestinoAnterior = cuentaDestino.getSaldoCuenta();

        try {
            cuentaOrigen.setSaldoCuenta(cuentaOrigen.getSaldoCuenta().subtract(cantidad));
            cuentaDestino.setSaldoCuenta(cuentaDestino.getSaldoCuenta().add(cantidad));

            registrarMovimiento(cuentaOrigen, TipoMovimiento.TRANSFERENCIA_SALIENTE, cantidad);
            registrarMovimiento(cuentaDestino, TipoMovimiento.TRANSFERENCIA_ENTRANTE, cantidad);
        } catch (RuntimeException ex) {
            cuentaOrigen.setSaldoCuenta(saldoOrigenAnterior);
            cuentaDestino.setSaldoCuenta(saldoDestinoAnterior);
            throw new ValidationException("Error durante la transferencia. Se han revertido los datos en memoria.");
        }
    }

    /**
     * Devuelve el historial completo de movimientos de una cuenta.
     *
     * @param numeroCuenta número de cuenta
     * @return lista de movimientos
     */
    public List<Movimiento> obtenerLista(String numeroCuenta) {
        Cuenta cuenta = obtenerCuentaValida(numeroCuenta);
        return repoMovi.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());
    }

    /**
     * Devuelve los movimientos de una cuenta filtrados por rango de fechas.
     *
     * @param numeroCuenta número de cuenta
     * @param fechaIn fecha inicial
     * @param fechaFin fecha final
     * @return lista de movimientos filtrados
     */
    public List<Movimiento> obtenerListaFecha(String numeroCuenta, LocalDate fechaIn, LocalDate fechaFin) {
        Cuenta cuenta = obtenerCuentaValida(numeroCuenta);

        if (fechaIn == null || fechaFin == null) {
            throw new ValidationException("Las fechas no pueden ser nulas");
        }

        if (fechaIn.isAfter(fechaFin)) {
            throw new ValidationException("La fecha de inicio no puede ser más antigua que la fecha final");
        }

        return repoMovi.obtenerMovimientosFecha(cuenta.getNumeroCuenta(), fechaIn, fechaFin);
    }

    private void registrarMovimiento(Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal cantidad) {
        Movimiento nuevoMovimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(tipoMovimiento)
                .cantidadMovimiento(cantidad)
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        repoMovi.guardarMovimiento(nuevoMovimiento);
    }

    private Cuenta obtenerCuentaValida(String numeroCuenta) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);
        validarNumeroCuenta(numeroNormalizado);

        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroNormalizado);

        if (cuenta == null) {
            throw new ResourceNotFoundException("No se ha encontrado la cuenta");
        }

        return cuenta;
    }

    private String normalizarNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null) {
            return null;
        }
        return numeroCuenta.trim().toUpperCase();
    }

    private void validarNumeroCuenta(String numeroCuenta) {
        if (!Utilidades.validarNumeroCuenta(numeroCuenta)) {
            throw new ValidationException("El número de cuenta debe tener formato ES seguido de 20 dígitos");
        }
    }

    private void validarCantidadPositiva(BigDecimal cantidad, String operacion) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("La cantidad a " + operacion + " debe ser mayor que cero");
        }
    }
}