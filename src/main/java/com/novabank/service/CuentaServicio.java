package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.memory.RepositorioCliente;
import com.novabank.persistence.memory.RepositorioCuenta;
import com.novabank.util.Utilidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de cuentas.
 *
 * Centraliza la lógica de negocio relacionada con creación, búsqueda y
 * consulta de cuentas, dejando al repositorio únicamente la persistencia
 * en memoria y al menú la interacción por consola.
 */
public class CuentaServicio {

    private final RepositorioCuenta repoCuenta;
    private final RepositorioCliente repoCliente;
    private long contadorNumCuentas = 0L;

    public CuentaServicio(RepositorioCuenta repoCuenta, RepositorioCliente repoCliente) {
        this.repoCuenta = repoCuenta;
        this.repoCliente = repoCliente;
    }

    /**
     * Recupera una cuenta por su número tras validar el formato de entrada.
     *
     * @param numeroCuenta número de cuenta a buscar
     * @return cuenta encontrada
     * @throws ValidationException si el formato no es válido
     * @throws ResourceNotFoundException si la cuenta no existe
     */
    public Cuenta buscarNumero(String numeroCuenta) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);
        validarNumeroCuenta(numeroNormalizado);

        Cuenta cuentaEncontrada = repoCuenta.buscarNumeroCuenta(numeroNormalizado);

        if (cuentaEncontrada == null) {
            throw new ResourceNotFoundException("No se ha encontrado ninguna cuenta con ese número.");
        }

        return cuentaEncontrada;
    }

    /**
     * Recupera el cliente titular por id.
     *
     * @param idCliente identificador del cliente
     * @return cliente encontrado
     * @throws ValidationException si el id es inválido
     * @throws ResourceNotFoundException si el cliente no existe
     */
    public Cliente obtenerTitular(Long idCliente) {
        validarIdCliente(idCliente);

        Cliente cliente = repoCliente.buscarIdCliente(idCliente);

        if (cliente == null) {
            throw new ResourceNotFoundException("No existe ningún cliente con ID " + idCliente + ".");
        }

        return cliente;
    }

    /**
     * Devuelve las cuentas asociadas a un cliente existente.
     *
     * @param idCliente identificador del cliente
     * @return lista de cuentas del cliente
     */
    public List<Cuenta> obtenerCuentas(Long idCliente) {
        Cliente cliente = obtenerTitular(idCliente);
        return repoCuenta.listarCuentasCliente(cliente.getIdCliente());
    }

    /**
     * Crea una cuenta nueva para un cliente existente con saldo inicial a cero.
     *
     * @param idCliente identificador del titular
     * @return cuenta creada
     */
    public Cuenta crearCuenta(Long idCliente) {
        Cliente cliente = obtenerTitular(idCliente);

        Cuenta nuevaCuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generadorNumero())
                .saldoCuenta(BigDecimal.ZERO)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repoCuenta.guardarCuenta(nuevaCuenta);
        return nuevaCuenta;
    }

    /**
     * Genera un número de cuenta interno con el formato usado en el proyecto.
     *
     * @return número de cuenta generado
     */
    public String generadorNumero() {
        return "ES91210000" + String.format("%012d", ++contadorNumCuentas);
    }

    private void validarIdCliente(Long idCliente) {
        if (idCliente == null || idCliente <= 0) {
            throw new ValidationException("Debes introducir un ID de cliente válido.");
        }
    }

    private String normalizarNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null) {
            return null;
        }
        return numeroCuenta.trim().toUpperCase();
    }

    private void validarNumeroCuenta(String numeroCuenta) {
        if (!Utilidades.validarNumeroCuenta(numeroCuenta)) {
            throw new ValidationException("El número de cuenta debe tener formato ES seguido de 20 dígitos");
        }
    }
}
