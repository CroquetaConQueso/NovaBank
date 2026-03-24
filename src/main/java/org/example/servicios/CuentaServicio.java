package org.example.servicios;

import org.example.modelos.Cliente;
import org.example.modelos.Cuenta;
import org.example.repositorio.RepositorioCliente;
import org.example.repositorio.RepositorioCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CuentaServicio {

    private final RepositorioCuenta repoCuenta;
    private final RepositorioCliente repoCliente;
    private static long contadorNumCuentas = 0L;

    public CuentaServicio(RepositorioCuenta repoCuenta, RepositorioCliente repoCliente) {
        this.repoCuenta = repoCuenta;
        this.repoCliente = repoCliente;
    }

    private void validarNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new IllegalArgumentException("Debes introducir un número de cuenta.");
        }else if (!numeroCuenta.matches("ES\\d{20}")) {
            throw new IllegalArgumentException("El número de cuenta debe tener formato IBAN simplificado: ES + 20 dígitos.");
        }
    }

    public Cuenta buscarNumero(String numeroCuenta){
        validarNumeroCuenta(numeroCuenta);
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta == null){
            throw new IllegalArgumentException("No se ha encontrado ninguna cuenta con ese número");
        }

        return cuenta;
    }


    public Cliente obtenerTitular(Long idCliente){
        if(idCliente <= 0){
            throw new IllegalArgumentException("Debes introducir un ID de cliente válido.");
        }

        Cliente cli = repoCliente.buscarIdCliente(idCliente);

        if(cli == null){
            throw new IllegalArgumentException("No existe ningúncliente con un ID "+idCliente+".");
        }
        return cli;
    }


    public List<Cuenta> obtenerCuentas(Long idCliente) {
        if (idCliente <= 0) {
            throw new IllegalArgumentException("Debes introducir un ID de cliente válido.");
        }

        Cliente cli = repoCliente.buscarIdCliente(idCliente);

        if (cli == null) {
            throw new IllegalArgumentException("No existe ningún cliente con ID " + idCliente + ".");
        }

        return repoCuenta.listarCuentasCliente(idCliente);
    }

    public String generadorNumero(){
        return "ES91210000" + String.format("%012d", ++contadorNumCuentas);
    }

    public Cuenta crearCuenta(Long idCliente){
        if(idCliente < 0) {
            throw new IllegalArgumentException("El valor debe de ser positivo");

        }
        Cliente cli = repoCliente.buscarIdCliente(idCliente);

        if(cli == null){
            throw new IllegalArgumentException("No existe ningún cliente con la id "+idCliente);
        }

        String numeroCuenta = generadorNumero();

        Cuenta nuevaCuenta = new Cuenta( cli,numeroCuenta, BigDecimal.ZERO, LocalDateTime.now());

        repoCuenta.guardarCuenta(nuevaCuenta);

        return nuevaCuenta;

    }


}
