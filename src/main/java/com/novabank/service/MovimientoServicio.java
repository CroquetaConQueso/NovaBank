package com.novabank.service;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.exception.NovaBankException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.jdbc.CuentaRepositoryJdbc;
import com.novabank.persistence.jdbc.MovimientoRepositoryJdbc;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;
import com.novabank.util.Utilidades;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de operaciones financieras.
 *
 * Centraliza la lógica de depósitos, retiros, transferencias y consulta
 * de movimientos.
 */
public class MovimientoServicio {

    private final CuentaRepository repoCuenta;
    private final MovimientoRepository repoMovi;

    public MovimientoServicio(CuentaRepository repoCuenta, MovimientoRepository repoMovi) {
        this.repoCuenta = repoCuenta;
        this.repoMovi = repoMovi;
    }

    public Cuenta depositar(String numeroCuenta, BigDecimal cantidad) {
        if (usaJdbcTransaccional()) {
            return depositarJdbc(numeroCuenta, cantidad);
        }

        Cuenta cuenta = obtenerCuentaValida(numeroCuenta);
        validarCantidadPositiva(cantidad, "depositar");

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().add(cantidad));
        registrarMovimiento(repoMovi, cuenta, TipoMovimiento.DEPOSITO, cantidad);

        return cuenta;
    }

    public Cuenta retirar(String numeroCuenta, BigDecimal cantidad) {
        if (usaJdbcTransaccional()) {
            return retirarJdbc(numeroCuenta, cantidad);
        }

        Cuenta cuenta = obtenerCuentaValida(numeroCuenta);
        validarCantidadPositiva(cantidad, "retirar");

        if (cuenta.getSaldoCuenta().compareTo(cantidad) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente.\nSaldo disponible: " + cuenta.getSaldoCuenta()
                            + " €\nImporte solicitado: " + cantidad + " €"
            );
        }

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().subtract(cantidad));
        registrarMovimiento(repoMovi, cuenta, TipoMovimiento.RETIRO, cantidad);

        return cuenta;
    }

    public void transferir(String numeroOrigen, String numeroDestino, BigDecimal cantidad) {
        if (usaJdbcTransaccional()) {
            transferirJdbc(numeroOrigen, numeroDestino, cantidad);
            return;
        }

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

            registrarMovimiento(repoMovi, cuentaOrigen, TipoMovimiento.TRANSFERENCIA_SALIENTE, cantidad);
            registrarMovimiento(repoMovi, cuentaDestino, TipoMovimiento.TRANSFERENCIA_ENTRANTE, cantidad);
        } catch (RuntimeException ex) {
            cuentaOrigen.setSaldoCuenta(saldoOrigenAnterior);
            cuentaDestino.setSaldoCuenta(saldoDestinoAnterior);
            throw new ValidationException("Error durante la transferencia. Se han revertido los datos en memoria.");
        }
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

        CuentaRepositoryJdbc cuentaJdbc = (CuentaRepositoryJdbc) repoCuenta;
        MovimientoRepositoryJdbc movimientoJdbc = (MovimientoRepositoryJdbc) repoMovi;

        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Cuenta cuenta = cuentaJdbc.buscarNumeroCuentaForUpdate(connection, numeroNormalizado);

                if (cuenta == null) {
                    throw new ResourceNotFoundException("No se ha encontrado la cuenta");
                }

                BigDecimal nuevoSaldo = cuenta.getSaldoCuenta().add(cantidad);
                cuentaJdbc.actualizarSaldo(connection, numeroNormalizado, nuevoSaldo);
                cuenta.setSaldoCuenta(nuevoSaldo);

                registrarMovimiento(connection, movimientoJdbc, cuenta, TipoMovimiento.DEPOSITO, cantidad);

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

        CuentaRepositoryJdbc cuentaJdbc = (CuentaRepositoryJdbc) repoCuenta;
        MovimientoRepositoryJdbc movimientoJdbc = (MovimientoRepositoryJdbc) repoMovi;

        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Cuenta cuenta = cuentaJdbc.buscarNumeroCuentaForUpdate(connection, numeroNormalizado);

                if (cuenta == null) {
                    throw new ResourceNotFoundException("No se ha encontrado la cuenta");
                }

                if (cuenta.getSaldoCuenta().compareTo(cantidad) < 0) {
                    throw new InsufficientBalanceException(
                            "Saldo insuficiente.\nSaldo disponible: " + cuenta.getSaldoCuenta()
                                    + " €\nImporte solicitado: " + cantidad + " €"
                    );
                }

                BigDecimal nuevoSaldo = cuenta.getSaldoCuenta().subtract(cantidad);
                cuentaJdbc.actualizarSaldo(connection, numeroNormalizado, nuevoSaldo);
                cuenta.setSaldoCuenta(nuevoSaldo);

                registrarMovimiento(connection, movimientoJdbc, cuenta, TipoMovimiento.RETIRO, cantidad);

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

        CuentaRepositoryJdbc cuentaJdbc = (CuentaRepositoryJdbc) repoCuenta;
        MovimientoRepositoryJdbc movimientoJdbc = (MovimientoRepositoryJdbc) repoMovi;

        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Cuenta cuentaOrigen = cuentaJdbc.buscarNumeroCuentaForUpdate(connection, origenNormalizado);
                Cuenta cuentaDestino = cuentaJdbc.buscarNumeroCuentaForUpdate(connection, destinoNormalizado);

                if (cuentaOrigen == null) {
                    throw new ResourceNotFoundException("No se ha encontrado la cuenta origen");
                }

                if (cuentaDestino == null) {
                    throw new ResourceNotFoundException("No se ha encontrado la cuenta destino");
                }

                if (cuentaOrigen.getSaldoCuenta().compareTo(cantidad) < 0) {
                    throw new InsufficientBalanceException(
                            "La cantidad a transferir no puede ser mayor que la cantidad encontrada en la cuenta"
                    );
                }

                BigDecimal saldoOrigenNuevo = cuentaOrigen.getSaldoCuenta().subtract(cantidad);
                BigDecimal saldoDestinoNuevo = cuentaDestino.getSaldoCuenta().add(cantidad);

                cuentaJdbc.actualizarSaldo(connection, origenNormalizado, saldoOrigenNuevo);
                cuentaJdbc.actualizarSaldo(connection, destinoNormalizado, saldoDestinoNuevo);

                cuentaOrigen.setSaldoCuenta(saldoOrigenNuevo);
                cuentaDestino.setSaldoCuenta(saldoDestinoNuevo);

                registrarMovimiento(connection, movimientoJdbc, cuentaOrigen, TipoMovimiento.TRANSFERENCIA_SALIENTE, cantidad);
                registrarMovimiento(connection, movimientoJdbc, cuentaDestino, TipoMovimiento.TRANSFERENCIA_ENTRANTE, cantidad);

                connection.commit();
            } catch (RuntimeException ex) {
                hacerRollbackSeguro(connection);
                throw ex;
            }
        } catch (SQLException ex) {
            throw new NovaBankException("Error de infraestructura durante la transferencia.", ex);
        }
    }

    private boolean usaJdbcTransaccional() {
        return repoCuenta instanceof CuentaRepositoryJdbc && repoMovi instanceof MovimientoRepositoryJdbc;
    }

    private void registrarMovimiento(MovimientoRepository repo, Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal cantidad) {
        Movimiento nuevoMovimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(tipoMovimiento)
                .cantidadMovimiento(cantidad)
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        repo.guardarMovimiento(nuevoMovimiento);
    }

    private void registrarMovimiento(Connection connection,
                                     MovimientoRepositoryJdbc repo,
                                     Cuenta cuenta,
                                     TipoMovimiento tipoMovimiento,
                                     BigDecimal cantidad) {
        Movimiento nuevoMovimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(tipoMovimiento)
                .cantidadMovimiento(cantidad)
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        repo.guardarMovimiento(connection, nuevoMovimiento);
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

    private void hacerRollbackSeguro(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            throw new NovaBankException("Error al intentar revertir la transacción.", rollbackEx);
        }
    }
}