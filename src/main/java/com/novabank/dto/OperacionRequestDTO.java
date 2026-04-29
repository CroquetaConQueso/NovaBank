package com.novabank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record OperacionRequestDTO(
        @NotBlank(message = "El numero de cuenta es obligatorio")
        @Pattern(regexp = "ES\\d{20}", message = "El numero de cuenta debe tener formato ES seguido de 20 digitos")
        String numeroCuenta,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
