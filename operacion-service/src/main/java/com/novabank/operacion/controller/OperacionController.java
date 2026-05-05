package com.novabank.operacion.controller;

import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.service.OperacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
