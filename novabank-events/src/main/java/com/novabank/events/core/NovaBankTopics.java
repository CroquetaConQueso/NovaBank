package com.novabank.events.core;

public final class NovaBankTopics {

    public static final String CLIENTES_REGISTRADOS = "novabank.clientes.registrados";
    public static final String OPERACIONES_SOLICITADAS = "novabank.operaciones.solicitadas";
    public static final String OPERACIONES_COMPLETADAS = "novabank.operaciones.completadas";
    public static final String OPERACIONES_FALLIDAS = "novabank.operaciones.fallidas";
    public static final String MOVIMIENTOS_REGISTRADOS = "novabank.movimientos.registrados";
    public static final String ALERTAS_SALDO_BAJO = "novabank.alertas.saldo-bajo";

    private NovaBankTopics() {
    }
}
