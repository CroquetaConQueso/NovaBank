package com.novabank.persistence.memory;

import com.novabank.domain.model.Movimiento;
import com.novabank.persistence.repository.MovimientoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio en memoria para movimientos financieros.
 */
public class RepositorioMovimiento implements MovimientoRepository {

    private final Map<Long, Movimiento> registroMovimientos = new HashMap<>();

    @Override
    public void guardarMovimiento(Movimiento nuevoMovimiento) {
        if (!registroMovimientos.containsKey(nuevoMovimiento.getIdMovimiento())) {
            registroMovimientos.put(nuevoMovimiento.getIdMovimiento(), nuevoMovimiento);
        } else {
            System.err.println("Ya existe un movimiento con esa id");
        }
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
        List<Movimiento> movimientosFiltrados = new ArrayList<>();

        for (Movimiento movimiento : registroMovimientos.values()) {
            if (movimiento.getCuentaAsignada().getNumeroCuenta().equals(numeroCuentaBuscar)) {
                LocalDate fechaMovimiento = movimiento.getFechaCreacionMov().toLocalDate();

                if (!fechaMovimiento.isBefore(inicio) && !fechaMovimiento.isAfter(fin)) {
                    movimientosFiltrados.add(movimiento);
                }
            }
        }

        movimientosFiltrados.sort(Comparator.comparing(Movimiento::getFechaCreacionMov).reversed());
        return movimientosFiltrados;
    }
}