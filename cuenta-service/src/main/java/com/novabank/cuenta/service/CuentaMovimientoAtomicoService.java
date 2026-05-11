package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.AplicarMovimientoRequestDTO;
import com.novabank.cuenta.dto.AplicarMovimientoResponseDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.exception.IdempotencyConflictException;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.EstadoOperacionIdempotente;
import com.novabank.cuenta.model.OperacionIdempotente;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.repository.OperacionIdempotenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class CuentaMovimientoAtomicoService {

    private final CuentaRepository cuentaRepository;
    private final OperacionIdempotenteRepository operacionIdempotenteRepository;
    private final CuentaMapper cuentaMapper;

    public CuentaMovimientoAtomicoService(
            CuentaRepository cuentaRepository,
            OperacionIdempotenteRepository operacionIdempotenteRepository,
            CuentaMapper cuentaMapper
    ) {
        this.cuentaRepository = cuentaRepository;
        this.operacionIdempotenteRepository = operacionIdempotenteRepository;
        this.cuentaMapper = cuentaMapper;
    }

    /**
     * Ejecuta la transferencia y el registro de idempotencia dentro de la misma
     * transaccion local de cuenta-service.
     */
    @Transactional
    public Mono<AplicarMovimientoResponseDTO> aplicarMovimiento(AplicarMovimientoRequestDTO request) {
        return Mono.defer(() -> {
            DatosMovimiento datos = validar(request);
            String requestHash = calcularHash(datos);

            return operacionIdempotenteRepository.findByOperationId(datos.operationId())
                    .flatMap(operacion -> resolverOperacionExistente(operacion, datos, requestHash))
                    .switchIfEmpty(Mono.defer(() -> registrarYAplicar(datos, requestHash)));
        });
    }

    private Mono<AplicarMovimientoResponseDTO> resolverOperacionExistente(
            OperacionIdempotente operacion,
            DatosMovimiento datos,
            String requestHash
    ) {
        if (!operacion.getRequestHash().equals(requestHash)) {
            return Mono.error(new IdempotencyConflictException(
                    "La operacion ya existe con una peticion diferente"
            ));
        }

        if (operacion.getEstado() == EstadoOperacionIdempotente.COMPLETED) {
            return buscarCuentas(datos)
                    .map(cuentas -> respuesta(
                            datos.operationId(),
                            "Operacion ya aplicada previamente",
                            cuentas.origen(),
                            cuentas.destino()
                    ));
        }

        return Mono.error(new IdempotencyConflictException(
                "La operacion ya fue registrada y no puede reutilizarse en este estado"
        ));
    }

    private Mono<AplicarMovimientoResponseDTO> registrarYAplicar(DatosMovimiento datos, String requestHash) {
        OperacionIdempotente operacion = OperacionIdempotente.builder()
                .operationId(datos.operationId())
                .requestHash(requestHash)
                .estado(EstadoOperacionIdempotente.PROCESSING)
                .build();
        operacion.prepararParaCreacion();

        return operacionIdempotenteRepository.save(operacion)
                .then(buscarCuentas(datos))
                .flatMap(cuentas -> aplicarTransferencia(cuentas, datos)
                        .then(Mono.defer(() -> {
                            operacion.marcarCompletada();
                            return operacionIdempotenteRepository.save(operacion);
                        }))
                        .thenReturn(respuesta(
                                datos.operationId(),
                                "Operacion aplicada correctamente",
                                cuentas.origen(),
                                cuentas.destino()
                        )));
    }

    private Mono<Void> aplicarTransferencia(CuentasMovimiento cuentas, DatosMovimiento datos) {
        Cuenta origen = cuentas.origen();
        Cuenta destino = cuentas.destino();

        if (origen.getSaldo().compareTo(datos.monto()) < 0) {
            return Mono.error(new InsufficientBalanceException(
                    "Saldo insuficiente. Saldo disponible: " + origen.getSaldo()
                            + " EUR. Importe solicitado: " + datos.monto() + " EUR."
            ));
        }

        origen.setSaldo(origen.getSaldo().subtract(datos.monto()));
        destino.setSaldo(destino.getSaldo().add(datos.monto()));

        return cuentaRepository.save(origen)
                .then(cuentaRepository.save(destino))
                .then();
    }

    private Mono<CuentasMovimiento> buscarCuentas(DatosMovimiento datos) {
        Mono<Cuenta> origen = cuentaRepository.findById(datos.cuentaOrigenId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "No existe ninguna cuenta con id " + datos.cuentaOrigenId()
                )));
        Mono<Cuenta> destino = cuentaRepository.findById(datos.cuentaDestinoId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "No existe ninguna cuenta con id " + datos.cuentaDestinoId()
                )));

        return Mono.zip(origen, destino)
                .map(tuple -> new CuentasMovimiento(tuple.getT1(), tuple.getT2()));
    }

    private AplicarMovimientoResponseDTO respuesta(
            String operationId,
            String mensaje,
            Cuenta cuentaOrigen,
            Cuenta cuentaDestino
    ) {
        CuentaResponseDTO origen = cuentaMapper.toResponse(cuentaOrigen);
        CuentaResponseDTO destino = cuentaMapper.toResponse(cuentaDestino);

        return new AplicarMovimientoResponseDTO(
                operationId,
                EstadoOperacionIdempotente.COMPLETED.name(),
                mensaje,
                origen,
                destino
        );
    }

    private DatosMovimiento validar(AplicarMovimientoRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la operacion son obligatorios");
        }

        String operationId = normalizarOperationId(request.operationId());
        Long origenId = validarId(request.cuentaOrigenId(), "El id de la cuenta origen debe ser positivo");
        Long destinoId = validarId(request.cuentaDestinoId(), "El id de la cuenta destino debe ser positivo");

        if (origenId.equals(destinoId)) {
            throw new IllegalArgumentException("La cuenta origen y destino deben ser diferentes");
        }

        BigDecimal monto = validarMonto(request.monto());
        String concepto = request.concepto() == null ? "" : request.concepto().trim();

        return new DatosMovimiento(operationId, origenId, destinoId, monto, concepto);
    }

    private String normalizarOperationId(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("El identificador de operacion es obligatorio");
        }

        return operationId.trim();
    }

    private Long validarId(Long id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }

        return id;
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }

        return monto;
    }

    private String calcularHash(DatosMovimiento datos) {
        String contenido = String.join("|",
                datos.operationId(),
                datos.cuentaOrigenId().toString(),
                datos.cuentaDestinoId().toString(),
                datos.monto().stripTrailingZeros().toPlainString(),
                datos.concepto().toLowerCase(Locale.ROOT)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash de idempotencia", ex);
        }
    }

    private record DatosMovimiento(
            String operationId,
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            BigDecimal monto,
            String concepto
    ) {
    }

    private record CuentasMovimiento(Cuenta origen, Cuenta destino) {
    }
}
