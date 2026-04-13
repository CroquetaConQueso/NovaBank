package com.novabank.persistence.repository;

import com.novabank.domain.model.Cuenta;

import java.util.List;

/**
 * Contrato de persistencia para cuentas.
 */
public interface CuentaRepository {

    void guardarCuenta(Cuenta nuevaCuenta);

    Cuenta buscarNumeroCuenta(String numeroCuenta);

    List<Cuenta> listarCuentasCliente(Long idBuscar);
}