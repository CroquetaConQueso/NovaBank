package com.novabank.persistence.repository;

import com.novabank.domain.model.Cuenta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia para cuentas.
 */
public interface CuentaRepository {

    void guardarCuenta(Cuenta nuevaCuenta);

    Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta);

    Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta, Connection connection);

    List<Cuenta> listarCuentasCliente(Long idBuscar);

    void actualizarSaldo(String numeroCuenta, BigDecimal nuevoSaldo);

    void actualizarSaldo(Connection connection, String numeroCuenta, BigDecimal nuevoSaldo);
}