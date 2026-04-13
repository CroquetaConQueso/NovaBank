package com.novabank.persistence.memory;

import com.novabank.domain.model.Cuenta;
import com.novabank.persistence.repository.CuentaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Cuenta buscarNumeroCuenta(String numeroCuenta) {
        return registroCuentas.values()
                .stream()
                .filter(cuenta -> cuenta.getNumeroCuenta().equals(numeroCuenta))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Cuenta> listarCuentasCliente(Long idBuscar) {
        return registroCuentas.values()
                .stream()
                .filter(cuenta -> cuenta.getDueñoCuenta().getIdCliente() == idBuscar)
                .toList();
    }
}