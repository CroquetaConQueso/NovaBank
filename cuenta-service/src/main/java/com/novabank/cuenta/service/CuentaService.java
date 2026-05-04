package com.novabank.cuenta.service;

import com.novabank.cuenta.client.ClienteServiceClient;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.MovimientoResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.exception.ValidationException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.mapper.MovimientoMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.Movimiento;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.repository.MovimientoRepository;
import com.novabank.cuenta.service.strategy.GeneradorNumeroCuentaStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final ClienteServiceClient clienteServiceClient;
    private final GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;
    private final MovimientoFactory movimientoFactory;
    private final CuentaMapper cuentaMapper;
    private final MovimientoMapper movimientoMapper;

    public CuentaService(
            CuentaRepository cuentaRepository,
            MovimientoRepository movimientoRepository,
            ClienteServiceClient clienteServiceClient,
            GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy,
            MovimientoFactory movimientoFactory,
            CuentaMapper cuentaMapper,
            MovimientoMapper movimientoMapper
    ) {
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.clienteServiceClient = clienteServiceClient;
        this.generadorNumeroCuentaStrategy = generadorNumeroCuentaStrategy;
        this.movimientoFactory = movimientoFactory;
        this.cuentaMapper = cuentaMapper;
        this.movimientoMapper = movimientoMapper;
    }

    @Transactional
    public CuentaResponseDTO crearCuenta(CuentaCreateRequestDTO request) {
        Long clienteId = validarId(request == null ? null : request.clienteId(), "El id del cliente debe ser positivo");
        clienteServiceClient.obtenerCliente(clienteId);

        Cuenta cuenta = new Cuenta();
        cuenta.setClienteId(clienteId);
        cuenta.setNumeroCuenta(generadorNumeroCuentaStrategy.generarNumeroCuenta());
        cuenta.setSaldo(BigDecimal.ZERO);

        return cuentaMapper.toResponse(cuentaRepository.save(cuenta));
    }

    @Transactional(readOnly = true)
    public CuentaResponseDTO obtenerCuenta(Long id) {
        return cuentaMapper.toResponse(buscarCuenta(id));
    }

    @Transactional(readOnly = true)
    public CuentaResponseDTO obtenerCuentaPorNumero(String numeroCuenta) {
        String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna cuenta con numero " + numeroNormalizado));

        return cuentaMapper.toResponse(cuenta);
    }

    @Transactional(readOnly = true)
    public SaldoResponseDTO consultarSaldo(Long id) {
        Cuenta cuenta = buscarCuenta(id);
        return new SaldoResponseDTO(cuenta.getId(), cuenta.getNumeroCuenta(), cuenta.getSaldo());
    }

    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> listarCuentasPorCliente(Long clienteId) {
        clienteId = validarId(clienteId, "El id del cliente debe ser positivo");
        clienteServiceClient.obtenerCliente(clienteId);

        return cuentaRepository.findByClienteId(clienteId)
                .stream()
                .map(cuentaMapper::toResponse)
                .toList();
    }

    @Transactional
    public MovimientoResponseDTO depositar(Long cuentaId, CuentaOperacionRequestDTO request) {
        BigDecimal cantidad = validarCantidad(request == null ? null : request.cantidad());
        Cuenta cuenta = buscarCuenta(cuentaId);

        cuenta.setSaldo(cuenta.getSaldo().add(cantidad));
        Movimiento movimiento = movimientoRepository.save(movimientoFactory.crearDeposito(cuenta, cantidad));

        return movimientoMapper.toResponse(movimiento);
    }

    @Transactional
    public MovimientoResponseDTO retirar(Long cuentaId, CuentaOperacionRequestDTO request) {
        BigDecimal cantidad = validarCantidad(request == null ? null : request.cantidad());
        Cuenta cuenta = buscarCuenta(cuentaId);
        validarSaldoSuficiente(cuenta, cantidad);

        cuenta.setSaldo(cuenta.getSaldo().subtract(cantidad));
        Movimiento movimiento = movimientoRepository.save(movimientoFactory.crearRetiro(cuenta, cantidad));

        return movimientoMapper.toResponse(movimiento);
    }

    @Transactional
    public List<MovimientoResponseDTO> transferir(TransferenciaInternaRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la transferencia son obligatorios");
        }

        Long origenId = validarId(request.cuentaOrigenId(), "El id de la cuenta origen debe ser positivo");
        Long destinoId = validarId(request.cuentaDestinoId(), "El id de la cuenta destino debe ser positivo");

        if (origenId.equals(destinoId)) {
            throw new IllegalArgumentException("La cuenta origen y destino deben ser diferentes");
        }

        BigDecimal cantidad = validarCantidad(request.cantidad());
        List<Cuenta> cuentas = cuentaRepository.findAllById(List.of(origenId, destinoId));
        Cuenta cuentaOrigen = buscarEnLista(cuentas, origenId);
        Cuenta cuentaDestino = buscarEnLista(cuentas, destinoId);
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
    public List<MovimientoResponseDTO> listarMovimientos(Long cuentaId, LocalDate fechaInicio, LocalDate fechaFin) {
        validarId(cuentaId, "El id de la cuenta debe ser positivo");

        if (fechaInicio == null && fechaFin == null) {
            if (!cuentaRepository.existsById(cuentaId)) {
                throw new ResourceNotFoundException("No existe ninguna cuenta con id " + cuentaId);
            }
            return movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId)
                    .stream()
                    .map(movimientoMapper::toResponse)
                    .toList();
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

    Cuenta buscarCuenta(Long id) {
        Long cuentaId = validarId(id, "El id de la cuenta debe ser positivo");

        return cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna cuenta con id " + cuentaId));
    }

    private Cuenta buscarEnLista(List<Cuenta> cuentas, Long id) {
        return cuentas.stream()
                .filter(cuenta -> id.equals(cuenta.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna cuenta con id " + id));
    }

    private String normalizarNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new ValidationException("El numero de cuenta es obligatorio");
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

    private BigDecimal validarCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            throw new IllegalArgumentException("La cantidad es obligatoria");
        }
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        return cantidad;
    }

    private Long validarId(Long id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }

        return id;
    }
}
