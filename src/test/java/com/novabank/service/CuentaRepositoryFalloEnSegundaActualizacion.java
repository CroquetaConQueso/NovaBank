package com.novabank.service;

import com.novabank.domain.model.Cuenta;
import com.novabank.exception.NovaBankException;
import com.novabank.persistence.repository.CuentaRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Test double que delega en un CuentaRepository real
 * y fuerza un fallo en la segunda actualización transaccional de saldo.
 */
public class CuentaRepositoryFalloEnSegundaActualizacion implements CuentaRepository {

    private final CuentaRepository delegate;
    private int contadorActualizacionesTransaccionales = 0;

    public CuentaRepositoryFalloEnSegundaActualizacion(CuentaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void guardarCuenta(Cuenta nuevaCuenta) {
        delegate.guardarCuenta(nuevaCuenta);
    }

    @Override
    public Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta) {
        return delegate.buscarNumeroCuenta(numeroCuenta);
    }

    @Override
    public Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta, Connection connection) {
        return delegate.buscarNumeroCuenta(numeroCuenta, connection);
    }

    @Override
    public List<Cuenta> listarCuentasCliente(Long idBuscar) {
        return delegate.listarCuentasCliente(idBuscar);
    }

    @Override
    public void actualizarSaldo(String numeroCuenta, BigDecimal nuevoSaldo) {
        delegate.actualizarSaldo(numeroCuenta, nuevoSaldo);
    }

    @Override
    public void actualizarSaldo(Connection connection, String numeroCuenta, BigDecimal nuevoSaldo) {
        contadorActualizacionesTransaccionales++;

        if (contadorActualizacionesTransaccionales == 2) {
            throw new NovaBankException("Fallo forzado en la segunda actualización de saldo.");
        }

        delegate.actualizarSaldo(connection, numeroCuenta, nuevoSaldo);
    }

    @Override
    public long obtenerUltimoIdCuenta() {
        return delegate.obtenerUltimoIdCuenta();
    }
}