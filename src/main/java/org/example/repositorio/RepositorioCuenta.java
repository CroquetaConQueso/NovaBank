package org.example.repositorio;

import org.example.modelos.Cuenta;

import java.util.HashMap;
import java.util.List;

public class RepositorioCuenta {
    //Duda
    private Long idsCuentas = 0L;
    private HashMap<String, Cuenta> registroCuentas = new HashMap<>();

    public void guardarCuenta(Cuenta nuevaCuenta){
        registroCuentas.put(nuevaCuenta.getNumeroCuenta(), nuevaCuenta);
    }

    public Cuenta buscarNumeroCuenta(String numeroCuenta){
        return registroCuentas.values().stream().filter(a->a.getNumeroCuenta()
                .equals(numeroCuenta)).findFirst()
                .orElse(null);
    }
    //Duda?
    public List<Cuenta> listarCuentasCliente(Long idBuscar){
        return registroCuentas.values().stream().filter(a -> a.getDueñoCuenta().getIdCliente() == idBuscar)
                .toList();
    }


}
