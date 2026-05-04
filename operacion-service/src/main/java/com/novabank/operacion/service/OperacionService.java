package com.novabank.operacion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.IdempotencyConflictException;
import com.novabank.operacion.exception.NovaBankException;
import com.novabank.operacion.exception.OperationAlreadyInProgressException;
import com.novabank.operacion.exception.PreviousOperationFailedException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.model.EstadoOperacion;
import com.novabank.operacion.model.OperacionIdempotente;
import com.novabank.operacion.model.TipoOperacion;
import com.novabank.operacion.repository.OperacionIdempotenteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;

@Service
public class OperacionService {

    private final CuentaServiceClient cuentaServiceClient;
    private final OperacionIdempotenteRepository operacionIdempotenteRepository;
    private final ObjectMapper objectMapper;

    public OperacionService(
            CuentaServiceClient cuentaServiceClient,
            OperacionIdempotenteRepository operacionIdempotenteRepository,
            ObjectMapper objectMapper
    ) {
        this.cuentaServiceClient = cuentaServiceClient;
        this.operacionIdempotenteRepository = operacionIdempotenteRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(noRollbackFor = NovaBankException.class)
    public OperacionResponseDTO depositar(
            OperacionRequestDTO request,
            String idempotencyKey,
            String correlationId
    ) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(TipoOperacion.DEPOSITO, request.cuentaId(), null, request.cantidad());

        return ejecutarConIdempotencia(
                normalizedKey,
                requestHash,
                TipoOperacion.DEPOSITO,
                request.cuentaId(),
                null,
                request.cantidad(),
                correlationId,
                () -> List.of(cuentaServiceClient.depositar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                ))
        );
    }

    @Transactional(noRollbackFor = NovaBankException.class)
    public OperacionResponseDTO retirar(
            OperacionRequestDTO request,
            String idempotencyKey,
            String correlationId
    ) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(TipoOperacion.RETIRO, request.cuentaId(), null, request.cantidad());

        return ejecutarConIdempotencia(
                normalizedKey,
                requestHash,
                TipoOperacion.RETIRO,
                request.cuentaId(),
                null,
                request.cantidad(),
                correlationId,
                () -> List.of(cuentaServiceClient.retirar(
                        request.cuentaId(),
                        new CuentaOperacionRequestDTO(request.cantidad())
                ))
        );
    }

    @Transactional(noRollbackFor = NovaBankException.class)
    public OperacionResponseDTO transferir(
            TransferenciaRequestDTO request,
            String idempotencyKey,
            String correlationId
    ) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(
                TipoOperacion.TRANSFERENCIA,
                request.cuentaOrigenId(),
                request.cuentaDestinoId(),
                request.cantidad()
        );

        return ejecutarConIdempotencia(
                normalizedKey,
                requestHash,
                TipoOperacion.TRANSFERENCIA,
                request.cuentaOrigenId(),
                request.cuentaDestinoId(),
                request.cantidad(),
                correlationId,
                () -> cuentaServiceClient.transferir(new TransferenciaInternaRequestDTO(
                        request.cuentaOrigenId(),
                        request.cuentaDestinoId(),
                        request.cantidad()
                ))
        );
    }

    private OperacionResponseDTO ejecutarConIdempotencia(
            String idempotencyKey,
            String requestHash,
            TipoOperacion tipoOperacion,
            Long cuentaOrigen,
            Long cuentaDestino,
            BigDecimal importe,
            String correlationId,
            Supplier<List<MovimientoResponseDTO>> remoteCall
    ) {
        return operacionIdempotenteRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> resolveExisting(existing, requestHash))
                .orElseGet(() -> executeNewOperation(
                        idempotencyKey,
                        requestHash,
                        tipoOperacion,
                        cuentaOrigen,
                        cuentaDestino,
                        importe,
                        correlationId,
                        remoteCall
                ));
    }

    private OperacionResponseDTO resolveExisting(OperacionIdempotente existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("La Idempotency-Key ya fue usada para otra operacion");
        }
        if (existing.getEstado() == EstadoOperacion.COMPLETADA) {
            return deserializeResponse(existing.getResponseBody());
        }
        if (existing.getEstado() == EstadoOperacion.FALLIDA) {
            throw new PreviousOperationFailedException(
                    "La operacion asociada a esta Idempotency-Key fallo previamente: " + existing.getErrorMessage()
            );
        }
        throw new OperationAlreadyInProgressException("La operacion asociada a esta Idempotency-Key esta en proceso");
    }

    private OperacionResponseDTO executeNewOperation(
            String idempotencyKey,
            String requestHash,
            TipoOperacion tipoOperacion,
            Long cuentaOrigen,
            Long cuentaDestino,
            BigDecimal importe,
            String correlationId,
            Supplier<List<MovimientoResponseDTO>> remoteCall
    ) {
        OperacionIdempotente operation = new OperacionIdempotente();
        operation.setIdempotencyKey(idempotencyKey);
        operation.setRequestHash(requestHash);
        operation.setTipoOperacion(tipoOperacion);
        operation.setEstado(EstadoOperacion.EN_PROCESO);
        operation.setCuentaOrigen(cuentaOrigen);
        operation.setCuentaDestino(cuentaDestino);
        operation.setImporte(importe);
        operation.setCorrelationId(correlationId);

        try {
            operation = operacionIdempotenteRepository.saveAndFlush(operation);
        } catch (DataIntegrityViolationException ex) {
            throw new OperationAlreadyInProgressException("La Idempotency-Key ya esta siendo procesada");
        }

        try {
            List<MovimientoResponseDTO> movimientos = remoteCall.get();
            OperacionResponseDTO response = new OperacionResponseDTO(
                    idempotencyKey,
                    tipoOperacion,
                    EstadoOperacion.COMPLETADA,
                    movimientos
            );
            operation.setEstado(EstadoOperacion.COMPLETADA);
            operation.setResponseBody(serializeResponse(response));
            operacionIdempotenteRepository.save(operation);
            return response;
        } catch (NovaBankException ex) {
            operation.setEstado(EstadoOperacion.FALLIDA);
            operation.setErrorMessage(ex.getMessage());
            operacionIdempotenteRepository.save(operation);
            throw ex;
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("La cabecera Idempotency-Key es obligatoria");
        }
        return idempotencyKey.trim();
    }

    private String requestHash(TipoOperacion tipoOperacion, Long cuentaOrigen, Long cuentaDestino, BigDecimal importe) {
        String canonicalPayload = tipoOperacion + "|"
                + cuentaOrigen + "|"
                + (cuentaDestino == null ? "" : cuentaDestino) + "|"
                + normalizeAmount(importe);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible", ex);
        }
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String serializeResponse(OperacionResponseDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la respuesta de operacion", ex);
        }
    }

    private OperacionResponseDTO deserializeResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, OperacionResponseDTO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo recuperar la respuesta idempotente", ex);
        }
    }
}
