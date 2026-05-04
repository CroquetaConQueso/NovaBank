package com.novabank.service;

import com.novabank.dto.MovimientoResponseDTO;
import com.novabank.dto.OperacionRequestDTO;
import com.novabank.dto.TransferenciaRequestDTO;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.mapper.MovimientoMapper;
import com.novabank.model.Cuenta;
import com.novabank.model.Movimiento;
import com.novabank.repository.CuentaRepository;
import com.novabank.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class OperacionService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final MovimientoFactory movimientoFactory;
    private final MovimientoMapper movimientoMapper;

    public OperacionService(
            CuentaRepository cuentaRepository,
            MovimientoRepository movimientoRepository,
            MovimientoFactory movimientoFactory,
            MovimientoMapper movimientoMapper
    ) {
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.movimientoFactory = movimientoFactory;
        this.movimientoMapper = movimientoMapper;
    }

    @Transactional
    public MovimientoResponseDTO depositar(OperacionRequestDTO request) {
        validarOperacionRequest(request);
        BigDecimal cantidad = validarCantidad(request.cantidad());
        Cuenta cuenta = buscarPorNumero(request.numeroCuenta());

        cuenta.setSaldo(cuenta.getSaldo().add(cantidad));
        Movimiento movimiento = movimientoRepository.save(
                movimientoFactory.crearDeposito(cuenta, cantidad)
        );

        return movimientoMapper.toResponse(movimiento);
    }

    @Transactional
    public MovimientoResponseDTO retirar(OperacionRequestDTO request) {
        validarOperacionRequest(request);
        BigDecimal cantidad = validarCantidad(request.cantidad());
        Cuenta cuenta = buscarPorNumero(request.numeroCuenta());
        validarSaldoSuficiente(cuenta, cantidad);

        cuenta.setSaldo(cuenta.getSaldo().subtract(cantidad));
        Movimiento movimiento = movimientoRepository.save(
                movimientoFactory.crearRetiro(cuenta, cantidad)
        );

        return movimientoMapper.toResponse(movimiento);
    }

    @Transactional
    public List<MovimientoResponseDTO> transferir(TransferenciaRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la transferencia son obligatorios");
        }

        String origen = normalizarNumeroCuenta(request.numeroCuentaOrigen());
        String destino = normalizarNumeroCuenta(request.numeroCuentaDestino());

        if (origen.equals(destino)) {
            throw new IllegalArgumentException("La cuenta origen y destino deben ser diferentes");
        }

        BigDecimal cantidad = validarCantidad(request.cantidad());
        Cuenta cuentaOrigen = buscarPorNumero(origen);
        Cuenta cuentaDestino = buscarPorNumero(destino);

        validarSaldoSuficiente(cuentaOrigen, cantidad);

        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(cantidad));
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(cantidad));

        Movimiento saliente = movimientoRepository.save(
                movimientoFactory.crearTransferenciaSaliente(cuentaOrigen, cantidad)
        );
        Movimiento entrante = movimientoRepository.save(
                movimientoFactory.crearTransferenciaEntrante(cuentaDestino, cantidad)
        );

        return List.of(saliente, entrante)
                .stream()
                .map(movimientoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponseDTO> listarMovimientos(Long cuentaId) {
        validarCuentaId(cuentaId);

        if (!cuentaRepository.existsById(cuentaId)) {
            throw new ResourceNotFoundException("No existe ninguna cuenta con id " + cuentaId);
        }

        return movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId)
                .stream()
                .map(movimientoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponseDTO> listarMovimientos(Long cuentaId, LocalDate fechaInicio, LocalDate fechaFin) {
        validarCuentaId(cuentaId);

        if (fechaInicio == null && fechaFin == null) {
            return listarMovimientos(cuentaId);
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Debe informar fechaInicio y fechaFin para filtrar por rango");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin");
        }
        if (!cuentaRepository.existsById(cuentaId)) {
            throw new ResourceNotFoundException("No existe ninguna cuenta con id " + cuentaId);
        }

        return movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                        cuentaId,
                        fechaInicio.atStartOfDay(),
                        fechaFin.atTime(LocalTime.MAX)
                )
                .stream()
                .map(movimientoMapper::toResponse)
                .toList();
    }

    private Cuenta buscarPorNumero(String numeroCuenta) {
        String normalizado = normalizarNumeroCuenta(numeroCuenta);

        return cuentaRepository.findByNumeroCuenta(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna cuenta con numero " + normalizado));
    }

    private String normalizarNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new IllegalArgumentException("El numero de cuenta es obligatorio");
        }

        return numeroCuenta.trim().toUpperCase(Locale.ROOT);
    }

    private void validarSaldoSuficiente(Cuenta cuenta, BigDecimal cantidad) {
        if (cuenta.getSaldo().compareTo(cantidad) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente. Saldo disponible: " + cuenta.getSaldo()
                            + " EUR. Importe solicitado: " + cantidad + " EUR."
            );
        }
    }

    private void validarOperacionRequest(OperacionRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la operacion son obligatorios");
        }
    }

    private BigDecimal validarCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            throw new IllegalArgumentException("La cantidad es obligatoria");
        }
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        return cantidad;
    }

    private void validarCuentaId(Long cuentaId) {
        if (cuentaId == null || cuentaId <= 0) {
            throw new IllegalArgumentException("El id de la cuenta debe ser positivo");
        }
    }
}
