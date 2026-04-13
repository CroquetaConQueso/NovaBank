package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.util.Utilidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de cuentas.
 *
 * Centraliza la lógica de negocio relacionada con creación, búsqueda y
 * consulta de cuentas, dejando al repositorio únicamente la persistencia
 * y al menú la interacción por consola.
 */
public class CuentaServicio {

    private final CuentaRepository repoCuenta;
    private final ClienteRepository repoCliente;
    private long contadorNumCuentas = 0L;

    public CuentaServicio(CuentaRepository repoCuenta, ClienteRepository repoCliente) {
        this.repoCuenta = repoCuenta;
        this.repoCliente = repoCliente;
    }

    public Cuenta buscarNumero(String numeroCuenta) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);
        validarNumeroCuenta(numeroNormalizado);

        Cuenta cuentaEncontrada = repoCuenta.buscarNumeroCuenta(numeroNormalizado);

        if (cuentaEncontrada == null) {
            throw new ResourceNotFoundException("No se ha encontrado ninguna cuenta con ese número.");
        }

        return cuentaEncontrada;
    }

    public Cliente obtenerTitular(Long idCliente) {
        validarIdCliente(idCliente);

        Cliente cliente = repoCliente.buscarIdCliente(idCliente);

        if (cliente == null) {
            throw new ResourceNotFoundException("No existe ningún cliente con ID " + idCliente + ".");
        }

        return cliente;
    }

    public List<Cuenta> obtenerCuentas(Long idCliente) {
        Cliente cliente = obtenerTitular(idCliente);
        return repoCuenta.listarCuentasCliente(cliente.getIdCliente());
    }

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