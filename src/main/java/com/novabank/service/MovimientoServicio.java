package com.novabank.service;
import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.factory.MovimientoFactory;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.exception.NovaBankException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;
import com.novabank.util.Utilidades;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de operaciones financieras.
 */
public class MovimientoServicio {
    private final CuentaRepository repoCuenta;
    private final MovimientoRepository repoMovi;

    public MovimientoServicio(CuentaRepository repoCuenta, MovimientoRepository repoMovi) {
        this.repoCuenta = repoCuenta;
        this.repoMovi = repoMovi;
    }

    public Cuenta depositar(String numeroCuenta, BigDecimal cantidad) {
        return depositarJdbc(numeroCuenta, cantidad);
    }

    public Cuenta retirar(String numeroCuenta, BigDecimal cantidad) {
        return retirarJdbc(numeroCuenta, cantidad);
    }

    public void transferir(String numeroOrigen, String numeroDestino, BigDecimal cantidad) {
        transferirJdbc(numeroOrigen, numeroDestino, cantidad);
    }

    public List<Movimiento> obtenerLista(String numeroCuenta) {
        Cuenta cuenta = obtenerCuentaValida(numeroCuenta);
        return repoMovi.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());
    }

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

    private Cuenta depositarJdbc(String numeroCuenta, BigDecimal cantidad) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);
        validarNumeroCuenta(numeroNormalizado);
        validarCantidadPositiva(cantidad, "depositar");
        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroNormalizado, connection).orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta"));
                BigDecimal nuevoSaldo = cuenta.getSaldoCuenta().add(cantidad);
                repoCuenta.actualizarSaldo(connection, numeroNormalizado, nuevoSaldo);
                cuenta.setSaldoCuenta(nuevoSaldo);
                registrarMovimiento(connection, cuenta, TipoMovimiento.DEPOSITO, cantidad);
                connection.commit();
                return cuenta;
            } catch (RuntimeException ex) {
                hacerRollbackSeguro(connection);
                throw ex;
            }
        } catch (SQLException ex) {
            throw new NovaBankException("Error de infraestructura durante el depósito.", ex);
        }
    }

    private Cuenta retirarJdbc(String numeroCuenta, BigDecimal cantidad) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);
        validarNumeroCuenta(numeroNormalizado);
        validarCantidadPositiva(cantidad, "retirar");
        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroNormalizado, connection).orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta"));
                if (cuenta.getSaldoCuenta().compareTo(cantidad) < 0) {
                    throw new InsufficientBalanceException("Saldo insuficiente.\nSaldo disponible: " + cuenta.getSaldoCuenta() + " €\nImporte solicitado: " + cantidad + " €");
                }
                BigDecimal nuevoSaldo = cuenta.getSaldoCuenta().subtract(cantidad);
                repoCuenta.actualizarSaldo(connection, numeroNormalizado, nuevoSaldo);
                cuenta.setSaldoCuenta(nuevoSaldo);
                registrarMovimiento(connection, cuenta, TipoMovimiento.RETIRO, cantidad);
                connection.commit();
                return cuenta;
            } catch (RuntimeException ex) {
                hacerRollbackSeguro(connection);
                throw ex;
            }
        } catch (SQLException ex) {
            throw new NovaBankException("Error de infraestructura durante el retiro.", ex);
        }
    }

    private void transferirJdbc(String numeroOrigen, String numeroDestino, BigDecimal cantidad) {
        String origenNormalizado = normalizarNumeroCuenta(numeroOrigen);
        String destinoNormalizado = normalizarNumeroCuenta(numeroDestino);
        validarNumeroCuenta(origenNormalizado);
        validarNumeroCuenta(destinoNormalizado);
        validarCantidadPositiva(cantidad, "transferir");
        if (origenNormalizado.equals(destinoNormalizado)) {
            throw new ValidationException("La cuenta origen y destino deben de ser diferentes");
        }
        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Cuenta cuentaOrigen = repoCuenta.buscarNumeroCuenta(origenNormalizado, connection).orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta origen"));
                Cuenta cuentaDestino = repoCuenta.buscarNumeroCuenta(destinoNormalizado, connection).orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta destino"));
                if (cuentaOrigen.getSaldoCuenta().compareTo(cantidad) < 0) {
                    throw new InsufficientBalanceException("La cantidad a transferir no puede ser mayor que la cantidad encontrada en la cuenta");
                }
                BigDecimal saldoOrigenNuevo = cuentaOrigen.getSaldoCuenta().subtract(cantidad);
                BigDecimal saldoDestinoNuevo = cuentaDestino.getSaldoCuenta().add(cantidad);
                repoCuenta.actualizarSaldo(connection, origenNormalizado, saldoOrigenNuevo);
                repoCuenta.actualizarSaldo(connection, destinoNormalizado, saldoDestinoNuevo);
                cuentaOrigen.setSaldoCuenta(saldoOrigenNuevo);
                cuentaDestino.setSaldoCuenta(saldoDestinoNuevo);
                registrarMovimiento(connection, cuentaOrigen, TipoMovimiento.TRANSFERENCIA_SALIENTE, cantidad);
                registrarMovimiento(connection, cuentaDestino, TipoMovimiento.TRANSFERENCIA_ENTRANTE, cantidad);
                connection.commit();
            } catch (RuntimeException ex) {
                hacerRollbackSeguro(connection);
                throw ex;
            }
        } catch (SQLException ex) {
            throw new NovaBankException("Error de infraestructura durante la transferencia.", ex);
        }
    }

    private void registrarMovimiento(Connection connection, Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal cantidad) {
        Movimiento nuevoMovimiento = crearMovimiento(cuenta, tipoMovimiento, cantidad);
        repoMovi.guardarMovimiento(connection, nuevoMovimiento);
    }

    private Movimiento crearMovimiento(Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal cantidad) {
        return switch (tipoMovimiento) {
            case DEPOSITO -> MovimientoFactory.crearDeposito(cuenta, cantidad);
            case RETIRO -> MovimientoFactory.crearRetiro(cuenta, cantidad);
            case TRANSFERENCIA_SALIENTE -> MovimientoFactory.crearTransferenciaSaliente(cuenta, cantidad);
            case TRANSFERENCIA_ENTRANTE -> MovimientoFactory.crearTransferenciaEntrante(cuenta, cantidad);
        };
    }

    private Cuenta obtenerCuentaValida(String numeroCuenta) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);
        validarNumeroCuenta(numeroNormalizado);
        return repoCuenta.buscarNumeroCuenta(numeroNormalizado).orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta"));
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

    private void hacerRollbackSeguro(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            throw new NovaBankException("Error al intentar revertir la transacción.", rollbackEx);
        }
    }
}