package com.novabank.exchangerate.controller;

import com.novabank.exchangerate.dto.ExchangeRateResponseDTO;
import com.novabank.exchangerate.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Locale;

@RestController
@RequestMapping("/api/exchange-rate")
@Tag(name = "Tipos de cambio", description = "Mock reactivo de proveedor externo de tasas de cambio")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar tasa de cambio mock",
            description = "Devuelve una tasa predefinida para pares soportados por el proveedor externo simulado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasa encontrada"),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos"),
            @ApiResponse(responseCode = "404", description = "Par de divisas no soportado"),
            @ApiResponse(responseCode = "500", description = "Error inesperado")
    })
    public Mono<ExchangeRateResponseDTO> obtenerTasa(
            @Parameter(description = "Moneda origen en formato ISO 4217", example = "USD")
            @RequestParam String from,
            @Parameter(description = "Moneda destino en formato ISO 4217", example = "EUR")
            @RequestParam String to
    ) {
        String fromNormalizado = normalizarDivisa(from, "from");
        String toNormalizado = normalizarDivisa(to, "to");

        return exchangeRateService.obtenerTasa(fromNormalizado, toNormalizado);
    }

    private String normalizarDivisa(String valor, String parametro) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El parametro " + parametro + " es obligatorio");
        }

        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
