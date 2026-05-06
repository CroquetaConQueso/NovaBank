package com.novabank.cuenta.controller;

import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@Tag(name = "Cuentas", description = "Gestion de cuentas bancarias y saldos")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Listar cuentas de un cliente", description = "Valida el cliente y devuelve sus cuentas asociadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuentas obtenidas correctamente"),
            @ApiResponse(responseCode = "400", description = "Identificador de cliente invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "503", description = "cliente-service no disponible")
    })
    public ResponseEntity<List<CuentaResponseDTO>> listarCuentasPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cuentaService.listarCuentasPorCliente(clienteId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cuenta por id", description = "Devuelve una cuenta usando su identificador interno.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "400", description = "Identificador invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaResponseDTO> obtenerCuenta(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.obtenerCuenta(id));
    }

    @GetMapping("/numero/{numeroCuenta}")
    @Operation(summary = "Obtener cuenta por numero", description = "Busca una cuenta por su numero bancario normalizado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "400", description = "Numero de cuenta invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaResponseDTO> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerCuentaPorNumero(numeroCuenta));
    }

    @PostMapping
    @Operation(summary = "Crear cuenta", description = "Crea una cuenta para un cliente existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "503", description = "cliente-service no disponible")
    })
    public ResponseEntity<CuentaResponseDTO> crearCuenta(@Valid @RequestBody CuentaCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.crearCuenta(request));
    }

    @GetMapping("/{id}/saldo")
    @Operation(summary = "Consultar saldo", description = "Devuelve el saldo actual de una cuenta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo obtenido correctamente"),
            @ApiResponse(responseCode = "400", description = "Identificador invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<SaldoResponseDTO> consultarSaldo(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(id));
    }

}
