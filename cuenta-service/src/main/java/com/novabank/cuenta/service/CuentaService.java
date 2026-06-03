package com.novabank.cuenta.service;

import com.novabank.cuenta.client.ClienteServiceClient;
import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.MovimientoEventDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.event.MovimientoRegistradoEventPublisher;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.exception.ValidationException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.service.strategy.GeneradorNumeroCuentaStrategy;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);

    private final CuentaRepository cuentaRepository;
    private final ClienteServiceClient clienteServiceClient;
    private final GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy;
    private final CuentaMapper cuentaMapper;
    private final MovimientoRegistradoEventPublisher movimientoRegistradoEventPublisher;

    public CuentaService(
            CuentaRepository cuentaRepository,
            ClienteServiceClient clienteServiceClient,
            GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy,
            CuentaMapper cuentaMapper,
            MovimientoRegistradoEventPublisher movimientoRegistradoEventPublisher
    ) {
        this.cuentaRepository = cuentaRepository;
        this.clienteServiceClient = clienteServiceClient;
        this.generadorNumeroCuentaStrategy = generadorNumeroCuentaStrategy;
        this.cuentaMapper = cuentaMapper;
        this.movimientoRegistradoEventPublisher = movimientoRegistradoEventPublisher;
    }

    @Transactional
    public Mono<CuentaResponseDTO> crearCuenta(CuentaCreateRequestDTO request) {
        return Mono.defer(() -> {
            Long clienteId = validarId(request == null ? null : request.clienteId(), "El id del cliente debe ser positivo");

            return clienteServiceClient.obtenerCliente(clienteId)
                    .then(Mono.defer(generadorNumeroCuentaStrategy::generarNumeroCuenta))
                    .map(numeroCuenta -> {
                        Cuenta cuenta = Cuenta.builder()
                                .clienteId(clienteId)
                                .numeroCuenta(numeroCuenta)
                                .saldo(BigDecimal.ZERO)
                                .build();
                        cuenta.prepararParaCreacion();
                        return cuenta;
                    })
                    .flatMap(cuentaRepository::save)
                    .doOnEach(signal -> logCuentaCreada(signal))
                    .map(cuentaMapper::toResponse);
        });
    }

    public Mono<CuentaResponseDTO> obtenerCuenta(Long id) {
        return buscarCuenta(id)
                .map(cuentaMapper::toResponse);
    }

    public Mono<CuentaResponseDTO> obtenerCuentaPorNumero(String numeroCuenta) {
        return Mono.defer(() -> {
            String numeroNormalizado = normalizarNumeroCuenta(numeroCuenta);

            return cuentaRepository.findByNumeroCuenta(numeroNormalizado)
                    .switchIfEmpty(Mono.error(
                            new ResourceNotFoundException("No existe ninguna cuenta con numero " + numeroNormalizado)
                    ))
                    .map(cuentaMapper::toResponse);
        });
    }

    public Mono<SaldoResponseDTO> consultarSaldo(Long id) {
        return buscarCuenta(id)
                .map(cuenta -> new SaldoResponseDTO(cuenta.getId(), cuenta.getNumeroCuenta(), cuenta.getSaldo()));
    }

    public Flux<CuentaResponseDTO> listarCuentasPorCliente(Long clienteId) {
        return Mono.defer(() -> {
                    Long id = validarId(clienteId, "El id del cliente debe ser positivo");
                    return clienteServiceClient.obtenerCliente(id).thenReturn(id);
                })
                .flatMapMany(cuentaRepository::findByClienteId)
                .map(cuentaMapper::toResponse);
    }

    /**
     * Actualiza saldo dentro de cuenta-service; el historial financiero lo
     * registra operacion-service en su propia base.
     */
    @Transactional
    public Mono<CuentaResponseDTO> depositar(Long cuentaId, CuentaOperacionRequestDTO request) {
        return Mono.defer(() -> {
            BigDecimal cantidad = validarCantidad(request == null ? null : request.cantidad());

            return buscarCuenta(cuentaId)
                    .map(cuenta -> {
                        cuenta.setSaldo(cuenta.getSaldo().add(cantidad));
                        return cuenta;
                    })
                    .flatMap(cuentaRepository::save)
                    .doOnEach(signal -> logMovimiento(signal, "DEPOSITO", cantidad))
                    .flatMap(cuenta -> publicarEvento(cuenta, "DEPOSITO", cantidad, "Deposito interno", null)
                            .thenReturn(cuenta))
                    .map(cuentaMapper::toResponse);
        });
    }

    /**
     * Aplica la regla de saldo suficiente antes de modificar la cuenta; el
     * movimiento asociado se crea fuera de este servicio.
     */
    @Transactional
    public Mono<CuentaResponseDTO> retirar(Long cuentaId, CuentaOperacionRequestDTO request) {
        return Mono.defer(() -> {
            BigDecimal cantidad = validarCantidad(request == null ? null : request.cantidad());

            return buscarCuenta(cuentaId)
                    .map(cuenta -> {
                        validarSaldoSuficiente(cuenta, cantidad);
                        cuenta.setSaldo(cuenta.getSaldo().subtract(cantidad));
                        return cuenta;
                    })
                    .flatMap(cuentaRepository::save)
                    .doOnEach(signal -> logMovimiento(signal, "RETIRO", cantidad))
                    .flatMap(cuenta -> publicarEvento(cuenta, "RETIRO", cantidad, "Retiro interno", null)
                            .thenReturn(cuenta))
                    .map(cuentaMapper::toResponse);
        });
    }

    /**
     * Mantiene la transferencia de saldos en una unica transaccion local de
     * cuenta-service, sin asumir responsabilidad sobre el historial.
     */
    @Transactional
    public Flux<CuentaResponseDTO> transferir(TransferenciaInternaRequestDTO request) {
        return Mono.defer(() -> {
                    if (request == null) {
                        return Mono.error(new IllegalArgumentException("Los datos de la transferencia son obligatorios"));
                    }

                    Long origenId = validarId(request.cuentaOrigenId(), "El id de la cuenta origen debe ser positivo");
                    Long destinoId = validarId(request.cuentaDestinoId(), "El id de la cuenta destino debe ser positivo");

                    if (origenId.equals(destinoId)) {
                        return Mono.error(new IllegalArgumentException("La cuenta origen y destino deben ser diferentes"));
                    }

                    BigDecimal cantidad = validarCantidad(request.cantidad());

                    return cuentaRepository.findAllById(List.of(origenId, destinoId))
                            .collectMap(Cuenta::getId)
                            .flatMap(cuentas -> aplicarTransferencia(cuentas, origenId, destinoId, cantidad));
                })
                .flatMapMany(cuentas -> cuentaRepository.saveAll(cuentas)
                        .concatMap(cuenta -> publicarEventoTransferencia(cuenta, origenDestinoTipo(cuenta, request), request.cantidad())
                                .thenReturn(cuenta))
                        .map(cuentaMapper::toResponse));
    }

    Mono<Cuenta> buscarCuenta(Long id) {
        return Mono.defer(() -> {
            Long cuentaId = validarId(id, "El id de la cuenta debe ser positivo");

            return cuentaRepository.findById(cuentaId)
                    .switchIfEmpty(Mono.error(
                            new ResourceNotFoundException("No existe ninguna cuenta con id " + cuentaId)
                    ));
        });
    }

    private Mono<List<Cuenta>> aplicarTransferencia(
            Map<Long, Cuenta> cuentas,
            Long origenId,
            Long destinoId,
            BigDecimal cantidad
    ) {
        Cuenta cuentaOrigen = buscarEnMapa(cuentas, origenId);
        Cuenta cuentaDestino = buscarEnMapa(cuentas, destinoId);
        validarSaldoSuficiente(cuentaOrigen, cantidad);

        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(cantidad));
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(cantidad));

        return Mono.just(List.of(cuentaOrigen, cuentaDestino));
    }

    private String origenDestinoTipo(Cuenta cuenta, TransferenciaInternaRequestDTO request) {
        if (cuenta.getId().equals(request.cuentaOrigenId())) {
            return "TRANSFERENCIA_SALIENTE";
        }
        return "TRANSFERENCIA_ENTRANTE";
    }

    private Mono<Void> publicarEventoTransferencia(Cuenta cuenta, String tipo, BigDecimal cantidad) {
        return publicarEvento(cuenta, tipo, cantidad, "Transferencia interna", null);
    }

    private Mono<Void> publicarEvento(
            Cuenta cuenta,
            String tipo,
            BigDecimal monto,
            String descripcion,
            String operationId
    ) {
        MovimientoEventDTO evento = new MovimientoEventDTO(
                cuenta.getId(),
                null,
                tipo,
                monto,
                cuenta.getSaldo(),
                descripcion,
                LocalDateTime.now(),
                operationId
        );

        return movimientoRegistradoEventPublisher.publicar(evento)
                .onErrorResume(error -> {
                    log.error(
                            "No se pudo publicar MovimientoRegistradoEvent cuentaId={} tipo={} operationId={}",
                            evento.cuentaId(),
                            evento.tipo(),
                            evento.operationId(),
                            error
                    );
                    return Mono.empty();
                });
    }

    private Cuenta buscarEnMapa(Map<Long, Cuenta> cuentas, Long id) {
        Cuenta cuenta = cuentas.get(id);
        if (cuenta == null) {
            throw new ResourceNotFoundException("No existe ninguna cuenta con id " + id);
        }
        return cuenta;
    }

    private String normalizarNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new ValidationException("El numero de cuenta es obligatorio");
        }

        return numeroCuenta.trim().toUpperCase(Locale.ROOT);
    }

    private void validarSaldoSuficiente(Cuenta cuenta, BigDecimal cantidad) {
        if (cuenta.getSaldo().compareTo(cantidad) < 0) {
            log.warn("saldo insuficiente cuentaId={} importe={}", cuenta.getId(), cantidad);
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

    private void logCuentaCreada(Signal<Cuenta> signal) {
        if (signal.isOnNext()) {
            log.info(
                    "correlationId={} cuenta creada id={} clienteId={}",
                    CorrelationIdSupport.fromContext(signal.getContextView()),
                    signal.get().getId(),
                    signal.get().getClienteId()
            );
        }
    }

    private void logMovimiento(Signal<Cuenta> signal, String tipo, BigDecimal cantidad) {
        if (signal.isOnNext()) {
            log.info(
                    "correlationId={} movimiento interno tipo={} cuentaId={} importe={}",
                    CorrelationIdSupport.fromContext(signal.getContextView()),
                    tipo,
                    signal.get().getId(),
                    cantidad
            );
        }
    }

}
