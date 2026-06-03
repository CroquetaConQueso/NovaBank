package com.novabank.cuenta.tracing;

import org.springframework.web.server.ServerWebExchange;
import reactor.util.context.ContextView;

import java.util.UUID;

public final class CorrelationIdSupport {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String CONTEXT_KEY = "correlationId";
    public static final String OPERATION_ID_CONTEXT_KEY = "operationId";

    private CorrelationIdSupport() {
    }

    public static String resolveOrCreate(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }

    public static String fromContext(ContextView contextView) {
        Object value = contextView.hasKey(CONTEXT_KEY) ? contextView.get(CONTEXT_KEY) : null;
        if (value instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }
        return null;
    }
}
