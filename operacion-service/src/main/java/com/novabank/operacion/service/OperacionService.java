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
    public OperacionResponseDTO depositar(OperacionRequestDTO request) {
        CuentaResponseDTO cuenta = cuentaServiceClient.depositar(
                request.cuentaId(),
                new CuentaOperacionRequestDTO(request.cantidad())
        );
        MovimientoResponseDTO movimiento = guardarMovimiento(cuenta, TipoMovimiento.DEPOSITO, request.cantidad());

        return new OperacionResponseDTO(
                "DEPOSITO",
                "Deposito realizado correctamente",
                List.of(movimiento)
        );
    }

    /**
     * Mantiene en cuenta-service la validacion de saldo suficiente y conserva
     * aqui el historial financiero resultante.
     */
    @Transactional
    public OperacionResponseDTO retirar(OperacionRequestDTO request) {
        CuentaResponseDTO cuenta = cuentaServiceClient.retirar(
                request.cuentaId(),
                new CuentaOperacionRequestDTO(request.cantidad())
        );
        MovimientoResponseDTO movimiento = guardarMovimiento(cuenta, TipoMovimiento.RETIRO, request.cantidad());

        return new OperacionResponseDTO(
                "RETIRO",
                "Retiro realizado correctamente",
                List.of(movimiento)
        );
    }

    /**
     * Solicita a cuenta-service una transferencia atomica de saldos y persiste
     * los dos movimientos que forman el historial de la operacion.
     */
    @Transactional
    public OperacionResponseDTO transferir(TransferenciaRequestDTO request) {
        List<CuentaResponseDTO> cuentas = cuentaServiceClient.transferir(new TransferenciaInternaRequestDTO(
                request.cuentaOrigenId(),
                request.cuentaDestinoId(),
                request.cantidad()
        ));
        CuentaResponseDTO cuentaOrigen = encontrarCuenta(cuentas, request.cuentaOrigenId());
        CuentaResponseDTO cuentaDestino = encontrarCuenta(cuentas, request.cuentaDestinoId());
        List<MovimientoResponseDTO> movimientos = List.of(
                guardarMovimiento(cuentaOrigen, TipoMovimiento.TRANSFERENCIA_SALIENTE, request.cantidad()),
                guardarMovimiento(cuentaDestino, TipoMovimiento.TRANSFERENCIA_ENTRANTE, request.cantidad())
        );

        return new OperacionResponseDTO(
                "TRANSFERENCIA",
                "Transferencia realizada correctamente",
                movimientos
        );
    }

    /**
     * El historial pertenece a operacion-service y se consulta mediante la
     * referencia logica de cuenta, sin foreign key entre bases de servicios.
     */
    @Transactional(readOnly = true)
    public List<MovimientoResponseDTO> listarMovimientos(Long cuentaId, LocalDate fechaInicio, LocalDate fechaFin) {
        validarId(cuentaId);

        if (fechaInicio == null && fechaFin == null) {
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

        return movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                        cuentaId,
                        fechaInicio.atStartOfDay(),
                        fechaFin.atTime(LocalTime.MAX)
                )
                .stream()
                .map(movimientoMapper::toResponse)
                .toList();
    }

    private MovimientoResponseDTO guardarMovimiento(
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

        return movimientoMapper.toResponse(movimientoRepository.save(movimiento));
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
