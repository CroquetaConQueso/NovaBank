package com.novabank.cuenta.service;

import com.novabank.cuenta.client.ClienteServiceClient;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.exception.ValidationException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.service.strategy.GeneradorNumeroCuentaStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteServiceClient clienteServiceClient;
    private final GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;
    private final CuentaMapper cuentaMapper;

    public CuentaService(
            CuentaRepository cuentaRepository,
            ClienteServiceClient clienteServiceClient,
            GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy,
            CuentaMapper cuentaMapper
    ) {
        this.cuentaRepository = cuentaRepository;
        this.clienteServiceClient = clienteServiceClient;
        this.generadorNumeroCuentaStrategy = generadorNumeroCuentaStrategy;
        this.cuentaMapper = cuentaMapper;
    }

    /**
     * Valida el cliente mediante cliente-service y persiste solo clienteId para
     * evitar relaciones JPA entre bases de datos de servicios distintos.
     */
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

    /**
     * Comprueba la existencia del cliente antes de devolver cuentas para que la
     * respuesta distinga entre cliente inexistente y cliente sin cuentas.
     */
    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> listarCuentasPorCliente(Long clienteId) {
        clienteId = validarId(clienteId, "El id del cliente debe ser positivo");
        clienteServiceClient.obtenerCliente(clienteId);

        return cuentaRepository.findByClienteId(clienteId)
                .stream()
                .map(cuentaMapper::toResponse)
                .toList();
    }

    /**
     * Actualiza saldo dentro de cuenta-service; el historial financiero lo
     * registra operacion-service en su propia base.
     */
    @Transactional
    public CuentaResponseDTO depositar(Long cuentaId, CuentaOperacionRequestDTO request) {
        BigDecimal cantidad = validarCantidad(request == null ? null : request.cantidad());
        Cuenta cuenta = buscarCuenta(cuentaId);

        cuenta.setSaldo(cuenta.getSaldo().add(cantidad));
        return cuentaMapper.toResponse(cuenta);
    }

    /**
     * Aplica la regla de saldo suficiente antes de modificar la cuenta; el
     * movimiento asociado se crea fuera de este servicio.
     */
    @Transactional
    public CuentaResponseDTO retirar(Long cuentaId, CuentaOperacionRequestDTO request) {
        BigDecimal cantidad = validarCantidad(request == null ? null : request.cantidad());
        Cuenta cuenta = buscarCuenta(cuentaId);
        validarSaldoSuficiente(cuenta, cantidad);

        cuenta.setSaldo(cuenta.getSaldo().subtract(cantidad));
        return cuentaMapper.toResponse(cuenta);
    }

    /**
     * Mantiene la transferencia de saldos en una unica transaccion local de
     * cuenta-service, sin asumir responsabilidad sobre el historial.
     */
    @Transactional
    public List<CuentaResponseDTO> transferir(TransferenciaInternaRequestDTO request) {
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

        return List.of(cuentaOrigen, cuentaDestino)
                .stream()
                .map(cuentaMapper::toResponse)
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
