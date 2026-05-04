package com.novabank.controller;

import com.novabank.dto.CuentaCreateRequestDTO;
import com.novabank.dto.CuentaResponseDTO;
import com.novabank.dto.MovimientoResponseDTO;
import com.novabank.dto.SaldoResponseDTO;
import com.novabank.service.CuentaService;
import com.novabank.service.OperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Cuentas", description = "Gestion de cuentas y consulta de movimientos")
public class CuentaController {

    private final CuentaService cuentaService;
    private final OperacionService operacionService;

    public CuentaController(CuentaService cuentaService, OperacionService operacionService) {
        this.cuentaService = cuentaService;
        this.operacionService = operacionService;
    }

    @PostMapping("/cuentas")
    @Operation(summary = "Crear cuenta", description = "Crea una cuenta bancaria asociada a un cliente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "Request invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<CuentaResponseDTO> crearCuenta(@Valid @RequestBody CuentaCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.crearCuenta(request));
    }

    @GetMapping("/cuentas/{id}")
    @Operation(summary = "Obtener cuenta por id", description = "Devuelve saldo y datos basicos de una cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "400", description = "Id invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaResponseDTO> obtenerCuenta(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.obtenerCuenta(id));
    }

    @GetMapping("/cuentas/numero/{numeroCuenta}")
    @Operation(summary = "Obtener cuenta por numero", description = "Devuelve saldo y datos basicos de una cuenta por su numero")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "400", description = "Numero de cuenta invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaResponseDTO> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerCuentaPorNumero(numeroCuenta));
    }

    @GetMapping("/cuentas/{id}/saldo")
    @Operation(summary = "Consultar saldo", description = "Devuelve el saldo actual de una cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo obtenido"),
            @ApiResponse(responseCode = "400", description = "Id invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "409", description = "Modificacion concurrente (optimistic locking)")
    })
    public ResponseEntity<SaldoResponseDTO> consultarSaldo(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(id));
    }

    @GetMapping("/clientes/{clienteId}/cuentas")
    @Operation(summary = "Listar cuentas de un cliente", description = "Devuelve las cuentas asociadas a un cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuentas obtenidas"),
            @ApiResponse(responseCode = "400", description = "Id invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<List<CuentaResponseDTO>> listarCuentasPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cuentaService.listarCuentasPorCliente(clienteId));
    }

    @GetMapping("/cuentas/{id}/movimientos")
    @Operation(
            summary = "Listar movimientos de una cuenta",
            description = "Devuelve todos los movimientos o filtra por fechaInicio y fechaFin"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos obtenidos"),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos o rango incompleto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<List<MovimientoResponseDTO>> listarMovimientos(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        return ResponseEntity.ok(operacionService.listarMovimientos(id, fechaInicio, fechaFin));
    }
}
