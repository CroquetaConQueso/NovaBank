package com.novabank.operacion.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.novabank.operacion.tracing.CorrelationIdSupport;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(correlationIdFilter());
    }

    private ExchangeFilterFunction correlationIdFilter() {
        return (request, next) -> Mono.deferContextual(contextView -> {
            Object value = contextView.hasKey(CorrelationIdSupport.CONTEXT_KEY)
                    ? contextView.get(CorrelationIdSupport.CONTEXT_KEY)
                    : null;
            String correlationId = value instanceof String id ? id : null;
            if (correlationId == null || correlationId.isBlank()) {
                return next.exchange(request);
            }

            ClientRequest tracedRequest = ClientRequest.from(request)
                    .header(CorrelationIdSupport.HEADER_NAME, correlationId)
                    .build();
            return next.exchange(tracedRequest);
        });
    }
}
