package com.novabank.operacion.controller;

import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.service.OperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Operaciones", description = "Operaciones financieras y consulta de movimientos")
public class OperacionController {

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @PostMapping("/deposito")
    @Operation(
            summary = "Realizar deposito",
            description = "Solicita a cuenta-service el cambio de saldo y registra el movimiento en operacion-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposito realizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "503", description = "cuenta-service no disponible")
    })
    public ResponseEntity<OperacionResponseDTO> depositar(
            @Valid @RequestBody OperacionRequestDTO request
    ) {
        return ResponseEntity.ok(operacionService.depositar(request));
    }

    @PostMapping("/retiro")
    @Operation(
            summary = "Realizar retiro",
            description = "Solicita a cuenta-service la validacion de saldo y registra el movimiento en operacion-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retiro realizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente"),
            @ApiResponse(responseCode = "503", description = "cuenta-service no disponible")
    })
    public ResponseEntity<OperacionResponseDTO> retirar(
            @Valid @RequestBody OperacionRequestDTO request
    ) {
        return ResponseEntity.ok(operacionService.retirar(request));
    }

    @PostMapping("/transferencia")
    @Operation(
            summary = "Realizar transferencia",
            description = "Solicita a cuenta-service la actualizacion de saldos y registra los movimientos de salida y entrada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferencia realizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cuenta origen o destino no encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente"),
            @ApiResponse(responseCode = "503", description = "cuenta-service no disponible")
    })
    public ResponseEntity<OperacionResponseDTO> transferir(
            @Valid @RequestBody TransferenciaRequestDTO request
    ) {
        return ResponseEntity.ok(operacionService.transferir(request));
    }

    @GetMapping("/cuentas/{cuentaId}/movimientos")
    @Operation(
            summary = "Listar movimientos de una cuenta",
            description = "Devuelve el historial registrado por operacion-service, con filtro opcional por fechas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos obtenidos correctamente"),
            @ApiResponse(responseCode = "400", description = "Identificador o rango de fechas invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway")
    })
    public ResponseEntity<List<MovimientoResponseDTO>> listarMovimientos(
            @PathVariable Long cuentaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        return ResponseEntity.ok(operacionService.listarMovimientos(cuentaId, fechaInicio, fechaFin));
    }
}
