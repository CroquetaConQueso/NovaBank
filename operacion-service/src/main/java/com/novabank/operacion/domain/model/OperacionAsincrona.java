package com.novabank.operacion.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OperacionAsincrona(
        UUID operationId,
        UUID correlationId,
        String tipoOperacion,
        Long cuentaId,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal importe,
        String moneda,
        EstadoOperacionAsincrona estado,
        String motivoFallo,
        LocalDateTime creadaEn,
        LocalDateTime actualizadaEn
) {

    public OperacionAsincrona marcarCompletada(LocalDateTime actualizadaEn) {
        return new OperacionAsincrona(
                operationId,
                correlationId,
                tipoOperacion,
                cuentaId,
                cuentaOrigenId,
                cuentaDestinoId,
                importe,
                moneda,
                EstadoOperacionAsincrona.COMPLETADA,
                null,
                creadaEn,
                actualizadaEn
        );
    }

    public OperacionAsincrona marcarFallida(String motivo, LocalDateTime actualizadaEn) {
        return new OperacionAsincrona(
                operationId,
                correlationId,
                tipoOperacion,
                cuentaId,
                cuentaOrigenId,
                cuentaDestinoId,
                importe,
                moneda,
                EstadoOperacionAsincrona.FALLIDA,
                motivo,
                creadaEn,
                actualizadaEn
        );
    }

    public boolean estaCompletada() {
        return estado == EstadoOperacionAsincrona.COMPLETADA;
    }

    public boolean estaFallida() {
        return estado == EstadoOperacionAsincrona.FALLIDA;
    }
}
