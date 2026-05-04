package com.novabank.operacion.controller;

import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.service.OperacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/operaciones")
public class OperacionController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @PostMapping("/deposito")
    public ResponseEntity<OperacionResponseDTO> depositar(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody OperacionRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(operacionService.depositar(request, idempotencyKey, correlationId(httpRequest)));
    }

    @PostMapping("/retiro")
    public ResponseEntity<OperacionResponseDTO> retirar(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody OperacionRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(operacionService.retirar(request, idempotencyKey, correlationId(httpRequest)));
    }

    @PostMapping("/transferencia")
    public ResponseEntity<OperacionResponseDTO> transferir(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody TransferenciaRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(operacionService.transferir(request, idempotencyKey, correlationId(httpRequest)));
    }

    private String correlationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
