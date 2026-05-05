package com.novabank.cuenta.controller;

import com.novabank.cuenta.dto.CuentaCreateRequestDTO;
import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.dto.SaldoResponseDTO;
import com.novabank.cuenta.service.CuentaService;
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
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<CuentaResponseDTO>> listarCuentasPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cuentaService.listarCuentasPorCliente(clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponseDTO> obtenerCuenta(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.obtenerCuenta(id));
    }

    @GetMapping("/numero/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDTO> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerCuentaPorNumero(numeroCuenta));
    }

    @PostMapping
    public ResponseEntity<CuentaResponseDTO> crearCuenta(@Valid @RequestBody CuentaCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.crearCuenta(request));
    }

    @GetMapping("/{id}/saldo")
    public ResponseEntity<SaldoResponseDTO> consultarSaldo(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(id));
    }

}
