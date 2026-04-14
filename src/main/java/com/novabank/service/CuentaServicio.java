package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.NovaBankException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.util.Utilidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servicio de cuentas.
 */
public class CuentaServicio {

    private final CuentaRepository repoCuenta;
    private final ClienteRepository repoCliente;

    public CuentaServicio(CuentaRepository repoCuenta, ClienteRepository repoCliente) {
        this.repoCuenta = repoCuenta;
        this.repoCliente = repoCliente;
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
                .numeroCuenta(generadorNumero())
                .saldoCuenta(BigDecimal.ZERO)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repoCuenta.guardarCuenta(nuevaCuenta);
        return nuevaCuenta;
    }

    public String generadorNumero() {
        for (int intento = 0; intento < 100; intento++) {
            long sufijo = ThreadLocalRandom.current().nextLong(1_000_000_000_000L);
            String numeroCuenta = "ES91210000" + String.format("%012d", sufijo);

            if (repoCuenta.buscarNumeroCuenta(numeroCuenta).isEmpty()) {
                return numeroCuenta;
            }
        }

        throw new NovaBankException("No se pudo generar un número de cuenta único.");
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