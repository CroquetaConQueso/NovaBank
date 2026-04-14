package com.novabank.persistence.repository;

import com.novabank.domain.model.Movimiento;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de persistencia para movimientos.
 */
public interface MovimientoRepository {

    void guardarMovimiento(Movimiento nuevoMovimiento);

    void guardarMovimiento(Connection connection, Movimiento nuevoMovimiento);

    List<Movimiento> obtenerMovimientosCuenta(String numeroCuentaBuscar);

    List<Movimiento> obtenerMovimientosFecha(String numeroCuentaBuscar, LocalDate inicio, LocalDate fin);
}