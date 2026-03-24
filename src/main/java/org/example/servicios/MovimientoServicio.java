package org.example.servicios;

import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.modelos.TipoMovimiento;
import org.example.repositorio.RepositorioCuenta;
import org.example.repositorio.RepositorioMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MovimientoServicio {

    private RepositorioCuenta repoCuenta;
    private RepositorioMovimiento repoMovi;

    public MovimientoServicio(RepositorioCuenta repoCuenta, RepositorioMovimiento repoMovi) {
        this.repoCuenta = repoCuenta;
        this.repoMovi = repoMovi;
    }

    private void registrarMovimiento(Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal cantidad){
        Movimiento nuevoMov = new Movimiento(cuenta, tipoMovimiento,cantidad, LocalDateTime.now());

        repoMovi.guardarMovimiento(nuevoMov);
    }

    public Cuenta depositar(String numeroCuenta, BigDecimal cantidad){
        if(cantidad ==null || cantidad.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("La cantidad a depositar debe ser mayor que cero");
        }
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta == null){
            throw new IllegalArgumentException("No se ha encontrado la cuenta");
        }

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().add(cantidad));
        registrarMovimiento(cuenta,TipoMovimiento.DEPOSITO,cantidad);

        return cuenta;
    }

    public Cuenta retirar(String numeroCuenta, BigDecimal cantidad){
        if(cantidad ==null || cantidad.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("La cantidad a retirar debe ser mayor que cero");
        }
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta == null){
            throw new IllegalArgumentException("No se ha encontrado la cuenta");
        }

        if (cuenta.getSaldoCuenta().compareTo(cantidad) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente.\nSaldo disponible: " + cuenta.getSaldoCuenta()
                    + " €\nImporte solicitado: " + cantidad + " €");
        }

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().subtract(cantidad));
        registrarMovimiento(cuenta,TipoMovimiento.RETIRO,cantidad);

        return cuenta;
    }

    public void transferir(String numeroOrigen, String numeroDestino, BigDecimal cantidad){
        if(cantidad ==null || cantidad.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("La cantidad a depositar debe ser mayor que cero");
        }else if(numeroOrigen.equals(numeroDestino)){
            throw new IllegalArgumentException("La cuenta origen y destino deben de ser diferentes");
        }

        Cuenta cuentaOrigen = repoCuenta.buscarNumeroCuenta(numeroOrigen);
        if(cuentaOrigen == null){
            throw new IllegalArgumentException("La cuenta a realizar la transferencia debe de existir");
        }
        Cuenta cuentaDestino = repoCuenta.buscarNumeroCuenta(numeroDestino);

        if(cuentaDestino == null){
            throw new IllegalArgumentException("La cuenta a la que se va a realizar la transferencia debe de existir");
        }

        if((cuentaOrigen.getSaldoCuenta().compareTo(cantidad))<0){
            throw new IllegalArgumentException("La cantidad a transferir no puede ser mayor que la cantidad encontrada en la cuenta");
        }
        try {
            cuentaOrigen.setSaldoCuenta(cuentaOrigen.getSaldoCuenta().subtract(cantidad));
            cuentaDestino.setSaldoCuenta(cuentaDestino.getSaldoCuenta().add(cantidad));
            registrarMovimiento(cuentaOrigen, TipoMovimiento.TRANSFERENCIA_SALIENTE, cantidad);
            registrarMovimiento(cuentaDestino, TipoMovimiento.TRANSFERENCIA_ENTRANTE, cantidad);
        }catch(Exception ex){
            cuentaOrigen.setSaldoCuenta(cuentaOrigen.getSaldoCuenta().add(cantidad));
            cuentaDestino.setSaldoCuenta(cuentaDestino.getSaldoCuenta().subtract(cantidad));
            throw new RuntimeException("Error durante la transferencia. Se han revertido los datos en memoria.");
        }
    }

    public List<Movimiento> obtenerLista(String numeroCuenta){
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta==null){
            throw new IllegalArgumentException("No se ha encontrado la cuenta");
        }
        return repoMovi.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());
    }

    public List<Movimiento> obtenerListaFecha(String numeroCuenta, LocalDate fechaIn, LocalDate fechaFin){
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta==null){
            throw new IllegalArgumentException("No se ha encontrado la cuenta");
        }

        if(fechaIn.isAfter(fechaFin)){
            throw new IllegalArgumentException("La fecha de inicio no puede ser más antigua que la fecha final");
        }else if(fechaFin.isBefore(fechaIn)){
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha inicial");
        }
        return repoMovi.obtenerMovimientosFecha(numeroCuenta,fechaIn,fechaFin);
    }
}
