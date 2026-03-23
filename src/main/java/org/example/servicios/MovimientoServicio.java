package org.example.servicios;

import lombok.AllArgsConstructor;
import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.modelos.TipoMovimiento;
import org.example.repositorio.RepositorioCuenta;
import org.example.repositorio.RepositorioMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
public class MovimientoServicio {

    private RepositorioCuenta repoCuenta;
    private RepositorioMovimiento repoMovi;

    private void registrarMovimiento(Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal cantidad){
        Movimiento nuevoMov = new Movimiento(cuenta, tipoMovimiento,cantidad, LocalDateTime.now());

        repoMovi.guardarMovimiento(nuevoMov);
    }

    public Cuenta depositar(String numeroCuenta, BigDecimal cantidad){
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta == null){
            throw new IllegalArgumentException("No se ha encontrado la cuenta");
        }

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().add(cantidad));
        registrarMovimiento(cuenta,TipoMovimiento.DEPOSITO,cantidad);

        return cuenta;
    }

    public Cuenta retirar(String numeroCuenta, BigDecimal cantidad){
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta == null){
            throw new IllegalArgumentException("No se ha encontrado la cuenta");
        }

        cuenta.setSaldoCuenta(cuenta.getSaldoCuenta().subtract(cantidad));
        registrarMovimiento(cuenta,TipoMovimiento.RETIRO,cantidad);

        return cuenta;
    }

    public void transferir(String numeroOrigen, String numeroDestino, BigDecimal cantidad){

        Cuenta cuentaOrigen = repoCuenta.buscarNumeroCuenta(numeroOrigen);
        Cuenta cuentaDestino = repoCuenta.buscarNumeroCuenta(numeroDestino);

        if(cuentaDestino == null){
            throw new IllegalArgumentException("La cuenta a la que se va a realizar la transferencia debe de existir");
        }

        if((cuentaOrigen.getSaldoCuenta().compareTo(cantidad))<0){
            throw new IllegalArgumentException("La cantidad a transferir no puede ser mayor que la cantidad encontrada en la cuenta");
        }

        cuentaOrigen.setSaldoCuenta(cuentaOrigen.getSaldoCuenta().subtract(cantidad));
        cuentaDestino.setSaldoCuenta(cuentaDestino.getSaldoCuenta().add(cantidad));
    }
}
