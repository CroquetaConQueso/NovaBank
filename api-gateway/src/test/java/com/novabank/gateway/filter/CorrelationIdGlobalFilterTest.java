package com.novabank.gateway.filter;

import com.novabank.gateway.tracing.CorrelationIdSupport;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void generaCorrelationIdCuandoNoLlegaHeader() {
        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes"));

        StepVerifier.create(filter.filter(exchange, chain(downstreamHeader))).verifyComplete();

        assertThat(downstreamHeader.get()).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdSupport.HEADER_NAME))
                .isEqualTo(downstreamHeader.get());
    }

    @Test
    void conservaCorrelationIdEntrante() {
        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes")
                .header(CorrelationIdSupport.HEADER_NAME, "cid-123"));

        StepVerifier.create(filter.filter(exchange, chain(downstreamHeader))).verifyComplete();

        assertThat(downstreamHeader.get()).isEqualTo("cid-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdSupport.HEADER_NAME)).isEqualTo("cid-123");
    }

    private GatewayFilterChain chain(AtomicReference<String> downstreamHeader) {
        return exchange -> {
            downstreamHeader.set(exchange.getRequest().getHeaders().getFirst(CorrelationIdSupport.HEADER_NAME));
            return Mono.empty();
        };
    }
}
