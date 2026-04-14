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
 * Mantiene una implementación simple basada en Map.
 * Aunque no usa transacciones reales, implementa las variantes
 * con Connection para respetar el contrato del repositorio.
 */
public class RepositorioCuenta implements CuentaRepository {

    private long contadorIds = 0L;
    private final Map<String, Cuenta> registroCuentas = new HashMap<>();

    @Override
    public void guardarCuenta(Cuenta nuevaCuenta) {
        Cuenta cuentaExistente = registroCuentas.get(nuevaCuenta.getNumeroCuenta());

        if (cuentaExistente != null && nuevaCuenta.getIdCuenta() <= 0) {
            nuevaCuenta.setIdCuenta(cuentaExistente.getIdCuenta());
        } else if (nuevaCuenta.getIdCuenta() <= 0) {
            nuevaCuenta.setIdCuenta(++contadorIds);
        }

        registroCuentas.put(nuevaCuenta.getNumeroCuenta(), nuevaCuenta);
    }

    @Override
    public Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta) {
        return Optional.ofNullable(registroCuentas.get(numeroCuenta));
    }

    @Override
    public Optional<Cuenta> buscarNumeroCuenta(String numeroCuenta, Connection connection) {
        return buscarNumeroCuenta(numeroCuenta);
    }

    @Override
    public List<Cuenta> listarCuentasCliente(Long idBuscar) {
        if (idBuscar == null) {
            return List.of();
        }

        return registroCuentas.values()
                .stream()
                .filter(cuenta -> cuenta.getDueñoCuenta() != null)
                .filter(cuenta -> cuenta.getDueñoCuenta().getIdCliente() == idBuscar.longValue())
                .toList();
    }

    @Override
    public void actualizarSaldo(String numeroCuenta, BigDecimal nuevoSaldo) {
        actualizarSaldo(null, numeroCuenta, nuevoSaldo);
    }

    @Override
    public void actualizarSaldo(Connection connection, String numeroCuenta, BigDecimal nuevoSaldo) {
        Cuenta cuenta = buscarNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta"));

        cuenta.setSaldoCuenta(nuevoSaldo);
    }
}