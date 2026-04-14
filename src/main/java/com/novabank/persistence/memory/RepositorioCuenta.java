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
 *
 * En memoria sí se asignan IDs manualmente porque no existe una base
 * de datos que los genere automáticamente.
 */
public class RepositorioCuenta implements CuentaRepository {

    private long contadorIds = 0L;
    private final Map<String, Cuenta> registroCuentas = new HashMap<>();

    @Override
    public void guardarCuenta(Cuenta nuevaCuenta) {
        if (nuevaCuenta.getIdCuenta() <= 0) {
            nuevaCuenta.setIdCuenta(++contadorIds);
        }

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
        return registroCuentas.values()
                .stream()
                .filter(cuenta -> cuenta.getDueñoCuenta().getIdCliente() == idBuscar)
                .toList();
    }
}