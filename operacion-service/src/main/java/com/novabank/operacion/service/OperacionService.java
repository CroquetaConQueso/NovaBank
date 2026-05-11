package com.novabank.operacion.service;

import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.mapper.MovimientoMapper;
import com.novabank.operacion.model.Movimiento;
import com.novabank.operacion.model.TipoMovimiento;
import com.novabank.operacion.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class OperacionService {

    private final CuentaServiceClient cuentaServiceClient;
    private final ExchangeRateService exchangeRateService;
    private final MovimientoRepository movimientoRepository;
    private final MovimientoMapper movimientoMapper;

    public OperacionService(
            CuentaServiceClient cuentaServiceClient,
            ExchangeRateService exchangeRateService,
            MovimientoRepository movimientoRepository,
            MovimientoMapper movimientoMapper
    ) {
        this.cuentaServiceClient = cuentaServiceClient;
        this.exchangeRateService = exchangeRateService;
        this.movimientoRepository = movimientoRepository;
        this.movimientoMapper = movimientoMapper;
    }

    /**
     * Delega el cambio de saldo en cuenta-service y registra el movimiento en
     * la base propia de operacion-service.
     */
    @Transactional
    public Mono<OperacionResponseDTO> depositar(OperacionRequestDTO request) {
        return cuentaServiceClient.depositar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                )
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .flatMap(cuenta -> guardarMovimiento(cuenta, TipoMovimiento.DEPOSITO, request.cantidad()))
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
        return cuentaServiceClient.retirar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                )
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .flatMap(cuenta -> guardarMovimiento(cuenta, TipoMovimiento.RETIRO, request.cantidad()))
                .map(movimiento -> new OperacionResponseDTO(
                        "RETIRO",
                        "Retiro realizado correctamente",
                        List.of(movimiento)
                ));
    }

    /**
     * Solicita a cuenta-service una transferencia atomica de saldos y persiste
     * los dos movimientos que forman el historial de la operacion.
     */
    @Transactional
    public Mono<OperacionResponseDTO> transferir(TransferenciaRequestDTO request) {
        return cuentaServiceClient.aplicarMovimiento(new AplicarMovimientoRequestDTO(
                        UUID.randomUUID().toString(),
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
                            .map(tuple -> new OperacionResponseDTO(
                                    "TRANSFERENCIA",
                                    "Transferencia realizada correctamente",
                                    List.of(tuple.getT1(), tuple.getT2())
                            ));
                });
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
}
