package com.novabank.operacion.controller;

import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.service.OperacionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/operaciones")
public class OperacionController {

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @PostMapping("/deposito")
    public ResponseEntity<OperacionResponseDTO> depositar(
            @Valid @RequestBody OperacionRequestDTO request
    ) {
        return ResponseEntity.ok(operacionService.depositar(request));
    }

    @PostMapping("/retiro")
    public ResponseEntity<OperacionResponseDTO> retirar(
            @Valid @RequestBody OperacionRequestDTO request
    ) {
        return ResponseEntity.ok(operacionService.retirar(request));
    }

    @PostMapping("/transferencia")
    public ResponseEntity<OperacionResponseDTO> transferir(
            @Valid @RequestBody TransferenciaRequestDTO request
    ) {
        return ResponseEntity.ok(operacionService.transferir(request));
    }

    @GetMapping("/cuentas/{cuentaId}/movimientos")
    public ResponseEntity<List<MovimientoResponseDTO>> listarMovimientos(
            @PathVariable Long cuentaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        return ResponseEntity.ok(operacionService.listarMovimientos(cuentaId, fechaInicio, fechaFin));
    }
}
