package org.example.repositorio;

import org.example.modelos.Movimiento;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class RepositorioMovimiento {
    private HashMap<Long, Movimiento> registroMovimientos = new HashMap<>();

    public void guardarMovimiento(Movimiento nuevoMovimiento){
        if(!registroMovimientos.containsKey(nuevoMovimiento.getIdMovimiento())){
            registroMovimientos.put(nuevoMovimiento.getIdMovimiento(),nuevoMovimiento);
        }else{
            System.err.println("Ya existe un movimiento con esa id");
        }
    }

    public Movimiento buscarIdMovimiento(Long idBuscar){
        return registroMovimientos.get(idBuscar);
    }

    public List<Movimiento> obtenerMovimientosCuenta(String numeroCuentaBuscar){
        return registroMovimientos.values().stream().filter(a -> a.getCuentaAsignada().getNumeroCuenta()
                .equals(numeroCuentaBuscar)).sorted(Comparator.comparing(Movimiento::getFechaCreacionMov).reversed()).toList();
    }

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

