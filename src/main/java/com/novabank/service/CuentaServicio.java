package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.service.strategy.GeneradorNumeroCuentaStrategy;
import com.novabank.util.Utilidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de cuentas.
 */
public class CuentaServicio {

    private final CuentaRepository repoCuenta;
    private final ClienteRepository repoCliente;
    private final GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;

    public CuentaServicio(
            CuentaRepository repoCuenta,
            ClienteRepository repoCliente,
            GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy
    ) {
        this.repoCuenta = repoCuenta;
        this.repoCliente = repoCliente;
        this.generadorNumeroCuentaStrategy = generadorNumeroCuentaStrategy;
    }

    public Cuenta buscarNumero(String numeroCuenta) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);
        validarNumeroCuenta(numeroNormalizado);

        return repoCuenta.buscarNumeroCuenta(numeroNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado ninguna cuenta con ese número."));
    }

    public Cliente obtenerTitular(Long idCliente) {
        validarIdCliente(idCliente);

        return repoCliente.buscarIdCliente(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ningún cliente con ID " + idCliente + "."));
    }

    public List<Cuenta> obtenerCuentas(Long idCliente) {
        Cliente cliente = obtenerTitular(idCliente);
        return repoCuenta.listarCuentasCliente(cliente.getIdCliente());
    }

    public Cuenta crearCuenta(Long idCliente) {
        Cliente cliente = obtenerTitular(idCliente);

        Cuenta nuevaCuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generadorNumeroCuentaStrategy.generarNumeroCuenta())
                .saldoCuenta(BigDecimal.ZERO)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repoCuenta.guardarCuenta(nuevaCuenta);
        return nuevaCuenta;
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