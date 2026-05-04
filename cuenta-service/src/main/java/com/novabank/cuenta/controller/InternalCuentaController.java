package com.novabank.cuenta.controller;

import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.MovimientoResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/cuentas")
public class InternalCuentaController {

    private final CuentaService cuentaService;

    public InternalCuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping("/{id}/depositos")
    public ResponseEntity<MovimientoResponseDTO> depositar(
            @PathVariable Long id,
            @Valid @RequestBody CuentaOperacionRequestDTO request
    ) {
        return ResponseEntity.ok(cuentaService.depositar(id, request));
    }

    @PostMapping("/{id}/retiros")
    public ResponseEntity<MovimientoResponseDTO> retirar(
            @PathVariable Long id,
            @Valid @RequestBody CuentaOperacionRequestDTO request
    ) {
        return ResponseEntity.ok(cuentaService.retirar(id, request));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<List<MovimientoResponseDTO>> transferir(
            @Valid @RequestBody TransferenciaInternaRequestDTO request
    ) {
        return ResponseEntity.ok(cuentaService.transferir(request));
    }
}
