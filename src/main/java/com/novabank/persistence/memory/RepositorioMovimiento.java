package com.novabank.persistence.memory;

import com.novabank.domain.model.Movimiento;
import com.novabank.persistence.repository.MovimientoRepository;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio en memoria para movimientos financieros.
 *
 * En memoria sí se asignan IDs manualmente porque no existe una base
 * de datos que los genere automáticamente.
 */
public class RepositorioMovimiento implements MovimientoRepository {

    private long contadorIds = 0L;
    private final Map<Long, Movimiento> registroMovimientos = new HashMap<>();

    @Override
    public void guardarMovimiento(Movimiento nuevoMovimiento) {
        if (nuevoMovimiento.getIdMovimiento() == null || nuevoMovimiento.getIdMovimiento() <= 0) {
            nuevoMovimiento.setIdMovimiento(++contadorIds);
        }

        registroMovimientos.put(nuevoMovimiento.getIdMovimiento(), nuevoMovimiento);
    }

    @Override
    public void guardarMovimiento(Connection connection, Movimiento nuevoMovimiento) {
        guardarMovimiento(nuevoMovimiento);
    }

    @Override
    public List<Movimiento> obtenerMovimientosCuenta(String numeroCuentaBuscar) {
        return registroMovimientos.values()
                .stream()
                .filter(movimiento -> movimiento.getCuentaAsignada().getNumeroCuenta().equals(numeroCuentaBuscar))
                .sorted(Comparator.comparing(Movimiento::getFechaCreacionMov).reversed())
                .toList();
    }

    @Override
    public List<Movimiento> obtenerMovimientosFecha(String numeroCuentaBuscar, LocalDate inicio, LocalDate fin) {
        return registroMovimientos.values()
                .stream()
                .filter(movimiento -> movimiento.getCuentaAsignada().getNumeroCuenta().equals(numeroCuentaBuscar))
                .filter(movimiento -> {
                    LocalDate fechaMovimiento = movimiento.getFechaCreacionMov().toLocalDate();
                    return !fechaMovimiento.isBefore(inicio) && !fechaMovimiento.isAfter(fin);
                })
                .sorted(Comparator.comparing(Movimiento::getFechaCreacionMov).reversed())
                .toList();
    }
}