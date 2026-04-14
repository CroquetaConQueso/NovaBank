package com.novabank.service;

import com.novabank.domain.model.Movimiento;
import com.novabank.exception.NovaBankException;
import com.novabank.persistence.repository.MovimientoRepository;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Test double que delega en un MovimientoRepository real
 * y fuerza un fallo en el segundo guardado transaccional de movimiento.
 */
public class MovimientoRepositoryFalloEnSegundoGuardado implements MovimientoRepository {

    private final MovimientoRepository delegate;
    private int contadorGuardadosTransaccionales = 0;

    public MovimientoRepositoryFalloEnSegundoGuardado(MovimientoRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void guardarMovimiento(Movimiento nuevoMovimiento) {
        delegate.guardarMovimiento(nuevoMovimiento);
    }

    @Override
    public void guardarMovimiento(Connection connection, Movimiento nuevoMovimiento) {
        contadorGuardadosTransaccionales++;

        if (contadorGuardadosTransaccionales == 2) {
            throw new NovaBankException("Fallo forzado en el segundo guardado de movimiento.");
        }

        delegate.guardarMovimiento(connection, nuevoMovimiento);
    }

    @Override
    public List<Movimiento> obtenerMovimientosCuenta(String numeroCuentaBuscar) {
        return delegate.obtenerMovimientosCuenta(numeroCuentaBuscar);
    }

    @Override
    public List<Movimiento> obtenerMovimientosFecha(String numeroCuentaBuscar, LocalDate inicio, LocalDate fin) {
        return delegate.obtenerMovimientosFecha(numeroCuentaBuscar, inicio, fin);
    }
}