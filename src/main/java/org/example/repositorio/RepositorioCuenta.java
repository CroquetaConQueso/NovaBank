package org.example.repositorio;

import org.example.modelos.Cuenta;

import java.util.HashMap;

public class RepositorioCuenta {
    //Duda
    private Long idsCuentas = 0L;
    private HashMap<String, Cuenta> registroCuentas = new HashMap<>();

    public void guardarCuenta(Cuenta nuevaCuenta){
        registroCuentas.put(nuevaCuenta.getNumeroCuenta(), nuevaCuenta);
    }

    public Cuenta buscarNumeroCuenta(Cuenta cuenta){
        return registroCuentas.values().stream().filter(a->a.getNumeroCuenta()
                .equals(cuenta.getNumeroCuenta())).findFirst()
                .orElseThrow(()-> new IllegalArgumentException("No se ha podido encontrar la cuenta"));
    }
    //Duda?
    public void listarCuentasCliente(Long idBuscar){
        registroCuentas.values().stream().filter(a -> a.getDueñoCuenta().getIdCliente() == idBuscar)
                .forEach(System.out::println);
    }


}
