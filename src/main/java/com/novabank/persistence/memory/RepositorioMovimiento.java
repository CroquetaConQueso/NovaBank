package com.novabank.persistence.memory;


import com.novabank.domain.model.Movimiento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Repositorio en memoria para la gestión de movimientos financieros.
 *
 * Almacena las operaciones realizadas sobre las cuentas y
 * permite su consulta por identificador, número de cuenta
 * o rango de fechas.
 */
public class RepositorioMovimiento {
    /**
     * Estructura de almacenamiento en memoria de los movimientos,
     * indexados por su identificador único.
     */
    private HashMap<Long, Movimiento> registroMovimientos = new HashMap<>();

    public void guardarMovimiento(Movimiento nuevoMovimiento){
        if(!registroMovimientos.containsKey(nuevoMovimiento.getIdMovimiento())){
            registroMovimientos.put(nuevoMovimiento.getIdMovimiento(),nuevoMovimiento);
        }else{
            System.err.println("Ya existe un movimiento con esa id");
        }
    }

    /**
     * Obtiene todos los movimientos asociados a una cuenta concreta,
     * ordenados por fecha de creación en orden descendente.
     *
     * @param numeroCuentaBuscar número de cuenta a consultar
     * @return lista de movimientos ordenados del más reciente al más antiguo
     */
    public List<Movimiento> obtenerMovimientosCuenta(String numeroCuentaBuscar){
        return registroMovimientos.values().stream().filter(a -> a.getCuentaAsignada().getNumeroCuenta()
                .equals(numeroCuentaBuscar)).sorted(Comparator.comparing(Movimiento::getFechaCreacionMov).reversed()).toList();
    }

    /**
     * Obtiene los movimientos de una cuenta dentro de un rango
     * de fechas específico (inclusive).
     *
     * @param numeroCuentaBuscar número de cuenta a consultar
     * @param inicio fecha inicial del rango
     * @param fin fecha final del rango
     * @return lista de movimientos dentro del intervalo indicado,
     *         ordenados del más reciente al más antiguo
     */
    public List<Movimiento> obtenerMovimientosFecha(String numeroCuentaBuscar, LocalDate inicio, LocalDate fin) {
        List<Movimiento> movimientosFiltrados = new ArrayList<>();

        for (Movimiento m : registroMovimientos.values()) {
            if (m.getCuentaAsignada().getNumeroCuenta().equals(numeroCuentaBuscar)) {
                LocalDate fechaMovimiento = m.getFechaCreacionMov().toLocalDate();

                if (!fechaMovimiento.isBefore(inicio) && !fechaMovimiento.isAfter(fin)) {
                    movimientosFiltrados.add(m);
                }
            }
        }
        movimientosFiltrados.sort((mov1, mov2) -> mov2.getFechaCreacionMov().compareTo(mov1.getFechaCreacionMov()));

        return movimientosFiltrados;
    }
}

