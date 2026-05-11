package com.novabank.gateway.tracing;

import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

public final class CorrelationIdSupport {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String EXCHANGE_ATTRIBUTE = "correlationId";

    private CorrelationIdSupport() {
    }

    public static String resolveOrCreate(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }

    public static String current(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(EXCHANGE_ATTRIBUTE);
        if (value instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }
        return exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
    }
}
