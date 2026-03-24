package org.example.repositorio;

import org.example.modelos.Cuenta;

import java.util.HashMap;
import java.util.List;

/**
 * Repositorio en memoria para la gestión de cuentas bancarias.
 *
 * Almacena las cuentas utilizando una estructura HashMap,
 * donde la clave corresponde al número de cuenta(OBJETO).
 */
public class RepositorioCuenta {
    /**
     * Estructura de almacenamiento en memoria de las cuentas,
     * indexadas por su número de cuenta.
     */
    private HashMap<String, Cuenta> registroCuentas = new HashMap<>();

    public void guardarCuenta(Cuenta nuevaCuenta){
        registroCuentas.put(nuevaCuenta.getNumeroCuenta(), nuevaCuenta);
    }

    public Cuenta buscarNumeroCuenta(String numeroCuenta){
        return registroCuentas.values().stream().filter(a->a.getNumeroCuenta()
                .equals(numeroCuenta)).findFirst()
                .orElse(null);
    }

    public List<Cuenta> listarCuentasCliente(Long idBuscar){
        return registroCuentas.values().stream().filter(a -> a.getDueñoCuenta().getIdCliente() == idBuscar)
                .toList();
    }


}
