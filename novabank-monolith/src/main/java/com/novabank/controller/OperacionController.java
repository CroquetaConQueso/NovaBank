package com.novabank.controller;

import com.novabank.dto.MovimientoResponseDTO;
import com.novabank.dto.OperacionRequestDTO;
import com.novabank.dto.TransferenciaRequestDTO;
import com.novabank.service.OperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operaciones")
@Tag(name = "Operaciones", description = "Depositos, retiros y transferencias")
public class OperacionController {

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @PostMapping("/deposito")
    @Operation(summary = "Registrar deposito", description = "Incrementa el saldo de una cuenta y registra el movimiento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposito registrado"),
            @ApiResponse(responseCode = "400", description = "Request invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<MovimientoResponseDTO> depositar(@Valid @RequestBody OperacionRequestDTO request) {
        return ResponseEntity.ok(operacionService.depositar(request));
    }

    @PostMapping("/retiro")
    @Operation(summary = "Registrar retiro", description = "Reduce el saldo de una cuenta si existe saldo suficiente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retiro registrado"),
            @ApiResponse(responseCode = "400", description = "Request invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public ResponseEntity<MovimientoResponseDTO> retirar(@Valid @RequestBody OperacionRequestDTO request) {
        return ResponseEntity.ok(operacionService.retirar(request));
    }

    @PostMapping("/transferencia")
    @Operation(summary = "Registrar transferencia", description = "Mueve saldo entre dos cuentas y registra ambos movimientos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferencia registrada"),
            @ApiResponse(responseCode = "400", description = "Request invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public ResponseEntity<List<MovimientoResponseDTO>> transferir(
            @Valid @RequestBody TransferenciaRequestDTO request
    ) {
        return ResponseEntity.ok(operacionService.transferir(request));
    }
}
