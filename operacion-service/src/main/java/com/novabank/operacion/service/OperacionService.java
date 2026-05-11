package com.novabank.operacion.service;

import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
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
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class OperacionService {

    private final CuentaServiceClient cuentaServiceClient;
    private final MovimientoRepository movimientoRepository;
    private final MovimientoMapper movimientoMapper;

    public OperacionService(
            CuentaServiceClient cuentaServiceClient,
            MovimientoRepository movimientoRepository,
            MovimientoMapper movimientoMapper
    ) {
        this.cuentaServiceClient = cuentaServiceClient;
        this.movimientoRepository = movimientoRepository;
        this.movimientoMapper = movimientoMapper;
    }

    /**
     * Delega el cambio de saldo en cuenta-service y registra el movimiento en
     * la base propia de operacion-service.
     */
    @Transactional
    public Mono<OperacionResponseDTO> depositar(OperacionRequestDTO request) {
        return Mono.defer(() -> cuentaServiceClientTransicionalDepositar(request))
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
        return Mono.defer(() -> cuentaServiceClientTransicionalRetirar(request))
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
        return Mono.defer(() -> cuentaServiceClientTransicionalTransferir(request))
                .flatMap(cuentas -> {
                    CuentaResponseDTO cuentaOrigen = encontrarCuenta(cuentas, request.cuentaOrigenId());
                    CuentaResponseDTO cuentaDestino = encontrarCuenta(cuentas, request.cuentaDestinoId());

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

    /**
     * Transicion temporal para #117: Feign sigue siendo bloqueante, por eso se
     * aisla en boundedElastic hasta sustituirlo por WebClient reactivo.
     */
    private Mono<CuentaResponseDTO> cuentaServiceClientTransicionalDepositar(OperacionRequestDTO request) {
        return Mono.fromCallable(() -> cuentaServiceClient.depositar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                ))
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Transicion temporal para #117: mantiene el contrato actual mientras la
     * capa web y la persistencia ya funcionan con Reactor.
     */
    private Mono<CuentaResponseDTO> cuentaServiceClientTransicionalRetirar(OperacionRequestDTO request) {
        return Mono.fromCallable(() -> cuentaServiceClient.retirar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                ))
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la cuenta")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Transicion temporal para #117: la llamada Feign se retirara cuando
     * operacion-service use WebClient balanceado hacia cuenta-service.
     */
    private Mono<List<CuentaResponseDTO>> cuentaServiceClientTransicionalTransferir(TransferenciaRequestDTO request) {
        return Mono.fromCallable(() -> cuentaServiceClient.transferir(new TransferenciaInternaRequestDTO(
                        request.cuentaOrigenId(),
                        request.cuentaDestinoId(),
                        request.cantidad()
                )))
                .switchIfEmpty(Mono.error(new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la transferencia")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private CuentaResponseDTO encontrarCuenta(List<CuentaResponseDTO> cuentas, Long cuentaId) {
        if (cuentas == null) {
            throw new RemoteResourceNotFoundException("cuenta-service no devolvio datos de la transferencia");
        }

        return cuentas.stream()
                .filter(cuenta -> cuentaId.equals(cuenta.id()))
                .findFirst()
                .orElseThrow(() -> new RemoteResourceNotFoundException("cuenta-service no devolvio la cuenta " + cuentaId));
    }

    private void validarId(Long cuentaId) {
        if (cuentaId == null || cuentaId <= 0) {
            throw new ValidationException("El id de la cuenta debe ser positivo");
        }
    }
}
