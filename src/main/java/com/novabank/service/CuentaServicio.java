package com.novabank.service;


import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.persistence.memory.RepositorioCliente;
import com.novabank.persistence.memory.RepositorioCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio encargado de la gestión de cuentas bancarias.
 *
 * Contiene la lógica de validación, creación y consulta de cuentas,
 * actuando como intermediario entre la capa de presentación
 * y los repositorios en memoria.
 */
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

    /**
     * Busca una cuenta por su número tras validar su formato.
     *
     * @param numeroCuenta número de cuenta a buscar
     * @return cuenta encontrada
     */
    public Cuenta buscarNumero(String numeroCuenta){
        validarNumeroCuenta(numeroCuenta);
        Cuenta cuenta = repoCuenta.buscarNumeroCuenta(numeroCuenta);

        if(cuenta == null){
            throw new IllegalArgumentException("No se ha encontrado ninguna cuenta con ese número");
        }

        return cuenta;
    }


    /**
     * Obtiene el cliente titular a partir de su identificador.
     *
     * @param idCliente identificador del cliente
     * @return cliente asociado
     */
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

    /**
     * Obtiene todas las cuentas asociadas a un cliente específico.
     *
     * @param idCliente identificador del cliente
     * @return lista de cuentas pertenecientes al cliente
     */
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

    /**
     * Genera un número de cuenta único utilizando
     * un contador incremental interno.
     *
     * @return número de cuenta con formato IBAN simplificado
     */
    public String generadorNumero(){
        return "ES91210000" + String.format("%012d", ++contadorNumCuentas);
    }

    /**
     * Crea una nueva cuenta bancaria asociada a un cliente existente.
     *
     * Inicializa el saldo en cero y asigna la fecha actual
     * como fecha de creación.
     *
     * @param idCliente identificador del cliente titular
     * @return nueva cuenta creada
     */
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
