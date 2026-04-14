package com.novabank.persistence.memory;

import com.novabank.domain.model.Cuenta;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.persistence.repository.CuentaRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio en memoria para cuentas bancarias.
 */
public class RepositorioCuenta implements CuentaRepository {

    private final Map<String, Cuenta> registroCuentas = new HashMap<>();

    @Override
    public void guardarCuenta(Cuenta nuevaCuenta) {
        registroCuentas.put(nuevaCuenta.getNumeroCuenta(), nuevaCuenta);
    }

    @Override
    public Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta) {
        return registroCuentas.values()
                .stream()
                .filter(cuenta -> cuenta.getNumeroCuenta().equals(numeroCuenta))
                .findFirst();
    }

    @Override
    public Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta, Connection connection) {
        return buscarNumeroCuenta(numeroCuenta);
    }

    @Override
    public void actualizarSaldo(Connection connection, String numeroCuenta, BigDecimal nuevoSaldo) {
        Cuenta cuenta = buscarNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta"));

        cuenta.setSaldoCuenta(nuevoSaldo);
    }

    @Override
    public List<Cuenta> listarCuentasCliente(Long idBuscar) {
        if (idBuscar == null) {
            return List.of();
        }

        return registroCuentas.values()
                .stream()
                .filter(cuenta -> cuenta.getDueñoCuenta().getIdCliente() == idBuscar.longValue())
                .toList();
    }
}