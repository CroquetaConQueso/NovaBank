package com.novabank.cuenta.controller;

import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/cuentas")
@Tag(name = "Cuentas internas", description = "Operaciones internas consumidas por operacion-service")
public class InternalCuentaController {

    private final CuentaService cuentaService;

    public InternalCuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping("/{id}/depositos")
    @Operation(
            summary = "Aplicar deposito interno",
            description = "Actualiza el saldo de una cuenta. Endpoint interno usado por operacion-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposito interno aplicado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public Mono<CuentaResponseDTO> depositar(
            @PathVariable Long id,
            @Valid @RequestBody CuentaOperacionRequestDTO request
    ) {
        return cuentaService.depositar(id, request);
    }

    @PostMapping("/{id}/retiros")
    @Operation(
            summary = "Aplicar retiro interno",
            description = "Valida saldo suficiente y actualiza la cuenta. Endpoint interno usado por operacion-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retiro interno aplicado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public Mono<CuentaResponseDTO> retirar(
            @PathVariable Long id,
            @Valid @RequestBody CuentaOperacionRequestDTO request
    ) {
        return cuentaService.retirar(id, request);
    }

    @PostMapping("/transferencias")
    @Operation(
            summary = "Aplicar transferencia interna",
            description = "Actualiza las cuentas origen y destino en una transaccion local. Endpoint interno usado por operacion-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferencia interna aplicada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "404", description = "Cuenta origen o destino no encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public Flux<CuentaResponseDTO> transferir(
            @Valid @RequestBody TransferenciaInternaRequestDTO request
    ) {
        return cuentaService.transferir(request);
    }
}
