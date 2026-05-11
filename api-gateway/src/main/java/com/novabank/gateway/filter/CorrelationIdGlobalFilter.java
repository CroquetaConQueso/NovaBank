package com.novabank.gateway.filter;

import com.novabank.gateway.tracing.CorrelationIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = CorrelationIdSupport.resolveOrCreate(exchange);
        exchange.getAttributes().put(CorrelationIdSupport.EXCHANGE_ATTRIBUTE, correlationId);

        ServerWebExchange tracedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(CorrelationIdSupport.HEADER_NAME, correlationId)))
                .build();
        tracedExchange.getResponse().getHeaders().set(CorrelationIdSupport.HEADER_NAME, correlationId);

        String path = tracedExchange.getRequest().getURI().getPath();
        log.info("correlationId={} request path={}", correlationId, path);

        return chain.filter(tracedExchange)
                .doFinally(signal -> log.info(
                        "correlationId={} response path={} status={}",
                        correlationId,
                        path,
                        tracedExchange.getResponse().getStatusCode()
                ))
                .contextWrite(context -> context.put(CorrelationIdSupport.EXCHANGE_ATTRIBUTE, correlationId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
