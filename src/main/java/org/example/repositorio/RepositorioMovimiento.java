package org.example.repositorio;

import org.example.modelos.Movimiento;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;

public class RepositorioMovimiento {
    private HashMap<Long, Movimiento> registroMovimientos = new HashMap<>();

    public void guardarMovimiento(Movimiento nuevoMovimiento){
        if(!registroMovimientos.containsKey(nuevoMovimiento.getIdMovimiento())){
            registroMovimientos.put(nuevoMovimiento.getIdMovimiento(),nuevoMovimiento);
        }else{
            System.out.println("Ya existe un movimiento con esa id");
        }
    }

    public Movimiento buscarIdMovimiento(Long idBuscar){
        return registroMovimientos.get(idBuscar);
    }

    public void listarMovimientosCuenta(String numeroCuentaBuscar){
        registroMovimientos.values().stream().filter(a -> a.getCuentaAsignada().getNumeroCuenta()
                .equals(numeroCuentaBuscar)).forEach(System.out::println);
    }

    public void listarMovimientosFecha(String numeroCuentaBuscar, String fechaInicio, String fechaFin) {
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate inicio = LocalDate.parse(fechaInicio, formatoFecha);
        LocalDate fin = LocalDate.parse(fechaFin, formatoFecha);

        registroMovimientos.values().stream().filter(m -> m.getCuentaAsignada().getNumeroCuenta().equals(numeroCuentaBuscar))
                .filter(m -> { LocalDate fechaMovimiento = m.getFechaCreacionMov().toLocalDate();
                    return !fechaMovimiento.isBefore(inicio) && !fechaMovimiento.isAfter(fin);})
                .sorted(Comparator.comparing(Movimiento::getFechaCreacionMov).reversed()).forEach(System.out::println);
    }
}
