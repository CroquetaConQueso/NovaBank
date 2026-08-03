package com.novabank.operacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaRequestDTO(
        @NotNull(message = "La cuenta origen es obligatoria")
        @Positive(message = "La cuenta origen debe ser positiva")
        Long cuentaOrigenId,

        @NotNull(message = "La cuenta destino es obligatoria")
        @Positive(message = "La cuenta destino debe ser positiva")
        Long cuentaDestinoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad,

        Boolean internacional,

        String paisDestino,

        String tipoCliente
) {

    public TransferenciaRequestDTO(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal cantidad) {
        this(cuentaOrigenId, cuentaDestinoId, cantidad, false, null, null);
    }

    @AssertTrue(message = "paisDestino es obligatorio para transferencias internacionales")
    public boolean isPaisDestinoInternacionalValido() {
        return !esInternacional() || hasText(paisDestino);
    }

    @AssertTrue(message = "tipoCliente es obligatorio para transferencias internacionales")
    public boolean isTipoClienteInternacionalValido() {
        return !esInternacional() || hasText(tipoCliente);
    }

    public boolean esInternacional() {
        return Boolean.TRUE.equals(internacional);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
