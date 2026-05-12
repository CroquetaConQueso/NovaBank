package com.novabank.auth.tracing;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = CorrelationIdSupport.resolveOrCreate(exchange);
        ServerWebExchange tracedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(CorrelationIdSupport.HEADER_NAME, correlationId)))
                .build();
        tracedExchange.getResponse().getHeaders().set(CorrelationIdSupport.HEADER_NAME, correlationId);

        return chain.filter(tracedExchange)
                .contextWrite(context -> context.put(CorrelationIdSupport.CONTEXT_KEY, correlationId));
    }
}
