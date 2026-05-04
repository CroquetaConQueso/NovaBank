package com.novabank.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generaCorrelationIdSiNoLlegaEnRequest() {
        AtomicReference<String> propagated = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes"));
        GatewayFilterChain chain = chainedExchange -> {
            propagated.set(chainedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(propagated.get()).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo(propagated.get());
    }

    @Test
    void respetaCorrelationIdEntrante() {
        AtomicReference<String> propagated = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-1"));
        GatewayFilterChain chain = chainedExchange -> {
            propagated.set(chainedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(propagated.get()).isEqualTo("corr-1");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo("corr-1");
    }
}
