package com.novabank.operacion.service;

import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionAceptadaResponseDTO;
import com.novabank.operacion.dto.OperacionEstadoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaAceptadaResponseDTO;
import com.novabank.operacion.dto.TransferenciaDivisaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.application.port.in.ConsultarEstadoOperacionQuery;
import com.novabank.operacion.application.port.in.ConsultarEstadoOperacionUseCase;
import com.novabank.operacion.application.port.in.EstadoOperacionAsincronaResult;
import com.novabank.operacion.application.port.in.OperacionAceptadaResult;
import com.novabank.operacion.application.port.in.SolicitarDepositoCommand;
import com.novabank.operacion.application.port.in.SolicitarDepositoUseCase;
import com.novabank.operacion.application.port.in.SolicitarRetiradaCommand;
import com.novabank.operacion.application.port.in.SolicitarRetiradaUseCase;
import com.novabank.operacion.application.port.in.SolicitarTransferenciaCommand;
import com.novabank.operacion.application.port.in.SolicitarTransferenciaUseCase;
import com.novabank.operacion.application.port.in.TransferenciaAceptadaResult;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.mapper.MovimientoMapper;
import com.novabank.operacion.model.Movimiento;
import com.novabank.operacion.model.TipoMovimiento;
import com.novabank.operacion.repository.MovimientoRepository;
import com.novabank.operacion.tracing.CorrelationIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class OperacionService {

    private static final Logger log = LoggerFactory.getLogger(OperacionService.class);

    private final CuentaServiceClient cuentaServiceClient;
    private final ExchangeRateService exchangeRateService;
    private final MovimientoRepository movimientoRepository;
    private final MovimientoMapper movimientoMapper;
    private final PublicIdempotencyService publicIdempotencyService;
    private final SolicitarDepositoUseCase solicitarDepositoUseCase;
    private final SolicitarRetiradaUseCase solicitarRetiradaUseCase;
    private final SolicitarTransferenciaUseCase solicitarTransferenciaUseCase;
    private final ConsultarEstadoOperacionUseCase consultarEstadoOperacionUseCase;

    public OperacionService(
            CuentaServiceClient cuentaServiceClient,
            ExchangeRateService exchangeRateService,
            MovimientoRepository movimientoRepository,
            MovimientoMapper movimientoMapper,
            PublicIdempotencyService publicIdempotencyService,
            SolicitarDepositoUseCase solicitarDepositoUseCase,
            SolicitarRetiradaUseCase solicitarRetiradaUseCase,
            SolicitarTransferenciaUseCase solicitarTransferenciaUseCase,
            ConsultarEstadoOperacionUseCase consultarEstadoOperacionUseCase
    ) {
        this.cuentaServiceClient = cuentaServiceClient;
        this.exchangeRateService = exchangeRateService;
        this.movimientoRepository = movimientoRepository;
        this.movimientoMapper = movimientoMapper;
        this.publicIdempotencyService = publicIdempotencyService;
        this.solicitarDepositoUseCase = solicitarDepositoUseCase;
        this.solicitarRetiradaUseCase = solicitarRetiradaUseCase;
        this.solicitarTransferenciaUseCase = solicitarTransferenciaUseCase;
        this.consultarEstadoOperacionUseCase = consultarEstadoOperacionUseCase;
    }

    public Mono<OperacionAceptadaResponseDTO> solicitarDeposito(
            OperacionRequestDTO request,
            String idempotencyKey
    ) {
        return Mono.deferContextual(contextView -> solicitarDepositoUseCase.solicitarDeposito(new SolicitarDepositoCommand(
                        request.cuentaId(),
                        request.cantidad(),
                        idempotencyKey,
                        resolveCorrelationId(CorrelationIdSupport.fromContext(contextView))
                )))
                .map(this::toOperacionAceptadaResponse);
    }

    public Mono<OperacionAceptadaResponseDTO> solicitarRetirada(
            OperacionRequestDTO request,
            String idempotencyKey
    ) {
        return Mono.deferContextual(contextView -> solicitarRetiradaUseCase.solicitarRetirada(new SolicitarRetiradaCommand(
                        request.cuentaId(),
                        request.cantidad(),
                        idempotencyKey,
                        resolveCorrelationId(CorrelationIdSupport.fromContext(contextView))
                )))
                .map(this::toOperacionAceptadaResponse);
    }

    public Mono<OperacionEstadoResponseDTO> consultarOperacionAsincrona(UUID operationId) {
        return consultarEstadoOperacionUseCase.consultar(new ConsultarEstadoOperacionQuery(operationId))
                .map(this::toOperacionEstadoResponse);
    }

    /**
     * Delega el cambio de saldo en cuenta-service y registra el movimiento en
     * la base propia de operacion-service.
     */
    @Transactional
    public Mono<OperacionResponseDTO> depositar(OperacionRequestDTO request) {
        return depositarCore(request);
    }

    @Transactional
    public Mono<OperacionResponseDTO> depositar(OperacionRequestDTO request, String idempotencyKey) {
        return publicIdempotencyService.execute(
                idempotencyKey,
                "DEPOSITO",
                hashDeposito(request),
                () -> depositarCore(request)
        );
    }

    private Mono<OperacionResponseDTO> depositarCore(OperacionRequestDTO request) {
        return cuentaServiceClient.depositar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                )
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .flatMap(cuenta -> guardarMovimiento(cuenta, TipoMovimiento.DEPOSITO, request.cantidad()))
                .doOnEach(signal -> logOperacion(signal, "DEPOSITO"))
                .map(movimiento -> new OperacionResponseDTO(
                        "DEPOSITO",
                        "Deposito realizado correctamente",
                        List.of(movimiento)
                ));
    }

    /**
     * Mantiene en cuenta-service la validacion de saldo suficiente y conserva
     * aqui el historial financiero resultante.
     */
    @Transactional
    public Mono<OperacionResponseDTO> retirar(OperacionRequestDTO request) {
        return retirarCore(request);
    }

    @Transactional
    public Mono<OperacionResponseDTO> retirar(OperacionRequestDTO request, String idempotencyKey) {
        return publicIdempotencyService.execute(
                idempotencyKey,
                "RETIRO",
                hashRetiro(request),
                () -> retirarCore(request)
        );
    }

    private Mono<OperacionResponseDTO> retirarCore(OperacionRequestDTO request) {
        return cuentaServiceClient.retirar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                )
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .flatMap(cuenta -> guardarMovimiento(cuenta, TipoMovimiento.RETIRO, request.cantidad()))
                .doOnEach(signal -> logOperacion(signal, "RETIRO"))
                .map(movimiento -> new OperacionResponseDTO(
                        "RETIRO",
                        "Retiro realizado correctamente",
                        List.of(movimiento)
                ));
    }

    public Mono<TransferenciaAceptadaResponseDTO> transferir(TransferenciaRequestDTO request) {
        return transferir(request, null);
    }

    public Mono<TransferenciaAceptadaResponseDTO> transferir(TransferenciaRequestDTO request, String idempotencyKey) {
        return solicitarTransferencia(request, idempotencyKey);
    }

    private Mono<TransferenciaAceptadaResponseDTO> solicitarTransferencia(
            TransferenciaRequestDTO request,
            String idempotencyKey
    ) {
        return Mono.deferContextual(contextView -> solicitarTransferenciaUseCase.solicitarTransferencia(
                        new SolicitarTransferenciaCommand(
                                request.cuentaOrigenId(),
                                request.cuentaDestinoId(),
                                request.cantidad(),
                                idempotencyKey,
                                resolveCorrelationId(CorrelationIdSupport.fromContext(contextView)),
                                request.internacional(),
                                request.paisDestino(),
                                request.tipoCliente()
                        )
                ))
                .map(this::toTransferenciaAceptadaResponse);
    }

    /**
     * Flujo sincronico conservado solo para transferencia en divisa hasta que
     * ese caso se migre a la SAGA del Modulo 6.
     */
    private Mono<OperacionResponseDTO> transferirCore(TransferenciaRequestDTO request, String operationId) {
        return cuentaServiceClient.aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        operationId,
                        request.cuentaOrigenId(),
                        request.cuentaDestinoId(),
                        request.cantidad(),
                        "Transferencia entre cuentas"
                ))
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la transferencia")))
                .flatMap(response -> {
                    CuentaResponseDTO cuentaOrigen = validarCuentaRemota(response.cuentaOrigen(), request.cuentaOrigenId());
                    CuentaResponseDTO cuentaDestino = validarCuentaRemota(response.cuentaDestino(), request.cuentaDestinoId());
                    Mono<MovimientoResponseDTO> movimientoOrigen = guardarMovimiento(
                            cuentaOrigen,
                            TipoMovimiento.TRANSFERENCIA_SALIENTE,
                            request.cantidad()
                    );
                    Mono<MovimientoResponseDTO> movimientoDestino = guardarMovimiento(
                            cuentaDestino,
                            TipoMovimiento.TRANSFERENCIA_ENTRANTE,
                            request.cantidad()
                    );

                    return Mono.zip(movimientoOrigen, movimientoDestino)
                            .doOnEach(signal -> logTransferencia(signal, "Transferencia realizada correctamente"))
                            .map(tuple -> new OperacionResponseDTO(
                                    "TRANSFERENCIA",
                                    "Transferencia realizada correctamente",
                                    List.of(tuple.getT1(), tuple.getT2())
                            ));
                });
    }

    /**
     * Consulta la tasa antes de tocar saldos; si el proveedor falla, el flujo
     * termina sin llamar a cuenta-service ni persistir movimientos.
     */
    @Transactional
    public Mono<OperacionResponseDTO> transferirEnDivisa(TransferenciaDivisaRequestDTO request) {
        return transferirEnDivisaCore(request, UUID.randomUUID().toString());
    }

    @Transactional
    public Mono<OperacionResponseDTO> transferirEnDivisa(TransferenciaDivisaRequestDTO request, String idempotencyKey) {
        String normalizedKey = publicIdempotencyService.normalizarKey(idempotencyKey);
        String operationId = normalizedKey == null ? UUID.randomUUID().toString() : operationIdDesdeIdempotencyKey(normalizedKey);

        return publicIdempotencyService.execute(
                normalizedKey,
                "TRANSFERENCIA_DIVISA",
                hashTransferenciaDivisa(request),
                () -> transferirEnDivisaCore(request, operationId)
        );
    }

    private Mono<OperacionResponseDTO> transferirEnDivisaCore(
            TransferenciaDivisaRequestDTO request,
            String operationId
    ) {
        return exchangeRateService.obtenerTasaConOrigen(request.monedaOrigen(), request.monedaDestino())
                .flatMap(rate -> {
                    BigDecimal montoConvertido = request.monto().multiply(rate.tasa()).setScale(2, RoundingMode.HALF_UP);
                    log.info(
                            "monto convertido para transferencia en divisa importe={} tasaCacheada={}",
                            montoConvertido,
                            rate.cacheada()
                    );
                    return transferirCore(
                        new TransferenciaRequestDTO(
                                request.cuentaOrigenId(),
                                request.cuentaDestinoId(),
                                montoConvertido
                        ),
                        operationId
                    ).map(response -> new OperacionResponseDTO(
                            response.tipoOperacion(),
                            mensajeTransferenciaDivisa(rate.cacheada()),
                            response.movimientos()
                    ));
                });
    }

    private String mensajeTransferenciaDivisa(boolean tasaCacheada) {
        if (tasaCacheada) {
            return "Transferencia en divisa realizada correctamente con tasa cacheada";
        }
        return "Transferencia en divisa realizada correctamente";
    }

    /**
     * El historial pertenece a operacion-service y se consulta mediante la
     * referencia logica de cuenta, sin foreign key entre bases de servicios.
     */
    @Transactional(readOnly = true)
    public Flux<MovimientoResponseDTO> listarMovimientos(Long cuentaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return Flux.defer(() -> {
            validarId(cuentaId);

            if (fechaInicio == null && fechaFin == null) {
                return movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId)
                        .map(movimientoMapper::toResponse);
            }
            if (fechaInicio == null || fechaFin == null) {
                throw new IllegalArgumentException("Debe informar fechaInicio y fechaFin para filtrar por rango");
            }
            if (fechaInicio.isAfter(fechaFin)) {
                throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin");
            }

            return movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                            cuentaId,
                            fechaInicio.atStartOfDay(),
                            fechaFin.atTime(LocalTime.MAX)
                    )
                    .map(movimientoMapper::toResponse);
        });
    }

    private Mono<MovimientoResponseDTO> guardarMovimiento(
            CuentaResponseDTO cuenta,
            TipoMovimiento tipo,
            BigDecimal cantidad
    ) {
        if (cuenta == null || cuenta.id() == null) {
            throw new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta");
        }

        Movimiento movimiento = new Movimiento();
        movimiento.setCuentaId(cuenta.id());
        movimiento.setNumeroCuenta(cuenta.numeroCuenta());
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.prepararParaCreacion();

        return movimientoRepository.save(movimiento)
                .map(movimientoMapper::toResponse);
    }

    private CuentaResponseDTO validarCuentaRemota(CuentaResponseDTO cuenta, Long cuentaId) {
        if (cuenta == null || cuenta.id() == null) {
            throw new RemoteResourceNotFoundException("cuenta-service no devolvio la cuenta " + cuentaId);
        }

        return cuenta;
    }

    private void validarId(Long cuentaId) {
        if (cuentaId == null || cuentaId <= 0) {
            throw new ValidationException("El id de la cuenta debe ser positivo");
        }
    }

    private String hashDeposito(OperacionRequestDTO request) {
        return publicIdempotencyService.hash(String.join("|",
                "DEPOSITO",
                String.valueOf(request.cuentaId()),
                normalizarImporte(request.cantidad())
        ));
    }

    private String hashRetiro(OperacionRequestDTO request) {
        return publicIdempotencyService.hash(String.join("|",
                "RETIRO",
                String.valueOf(request.cuentaId()),
                normalizarImporte(request.cantidad())
        ));
    }

    private String hashTransferencia(TransferenciaRequestDTO request) {
        return publicIdempotencyService.hash(String.join("|",
                "TRANSFERENCIA",
                String.valueOf(request.cuentaOrigenId()),
                String.valueOf(request.cuentaDestinoId()),
                normalizarImporte(request.cantidad())
        ));
    }

    private String hashTransferenciaDivisa(TransferenciaDivisaRequestDTO request) {
        return publicIdempotencyService.hash(String.join("|",
                "TRANSFERENCIA_DIVISA",
                String.valueOf(request.cuentaOrigenId()),
                String.valueOf(request.cuentaDestinoId()),
                normalizarImporte(request.monto()),
                normalizarMoneda(request.monedaOrigen()),
                normalizarMoneda(request.monedaDestino())
        ));
    }

    private String operationIdDesdeIdempotencyKey(String idempotencyKey) {
        return "public-" + publicIdempotencyService.hash("operation|" + idempotencyKey);
    }

    private String normalizarImporte(BigDecimal importe) {
        return importe == null ? "" : importe.stripTrailingZeros().toPlainString();
    }

    private String normalizarMoneda(String moneda) {
        return moneda == null ? "" : moneda.trim().toUpperCase();
    }

    private UUID resolveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID();
        }

        try {
            return UUID.fromString(correlationId);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }

    private OperacionAceptadaResponseDTO toOperacionAceptadaResponse(OperacionAceptadaResult result) {
        return new OperacionAceptadaResponseDTO(
                result.operationId(),
                result.estado(),
                result.mensaje(),
                result.tipoOperacion(),
                result.cuentaId(),
                result.importe()
        );
    }

    private TransferenciaAceptadaResponseDTO toTransferenciaAceptadaResponse(TransferenciaAceptadaResult result) {
        return new TransferenciaAceptadaResponseDTO(
                result.operationId(),
                result.estado(),
                result.mensaje(),
                result.tipoOperacion(),
                result.cuentaOrigenId(),
                result.cuentaDestinoId(),
                result.importe(),
                result.moneda(),
                result.comision(),
                result.tasaComision()
        );
    }

    private OperacionEstadoResponseDTO toOperacionEstadoResponse(EstadoOperacionAsincronaResult result) {
        return new OperacionEstadoResponseDTO(
                result.operationId(),
                result.correlationId(),
                result.tipoOperacion(),
                result.estado(),
                result.cuentaId(),
                result.cuentaOrigenId(),
                result.cuentaDestinoId(),
                result.importe(),
                result.moneda(),
                result.motivoFallo(),
                result.creadaEn(),
                result.actualizadaEn()
        );
    }

    private void logOperacion(Signal<MovimientoResponseDTO> signal, String tipo) {
        if (signal.isOnNext()) {
            log.info(
                    "correlationId={} operacion={} cuentaId={} movimientoId={}",
                    CorrelationIdSupport.fromContext(signal.getContextView()),
                    tipo,
                    signal.get().cuentaId(),
                    signal.get().id()
            );
        }
    }

    private void logTransferencia(Signal<?> signal, String mensaje) {
        if (signal.isOnNext()) {
            log.info(
                    "correlationId={} transferencia completada detalle={}",
                    CorrelationIdSupport.fromContext(signal.getContextView()),
                    mensaje
            );
        }
    }
}
