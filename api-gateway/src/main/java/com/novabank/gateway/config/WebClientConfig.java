package com.novabank.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.novabank.gateway.tracing.CorrelationIdSupport;

@Configuration
public class WebClientConfig {

    /**
     * Habilita la resolucion de nombres lb:// para que WebClient pueda llamar a
     * servicios registrados en Eureka.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .filter(correlationIdFilter());
    }

    private ExchangeFilterFunction correlationIdFilter() {
        return (request, next) -> Mono.deferContextual(contextView -> {
            Object value = contextView.hasKey(CorrelationIdSupport.EXCHANGE_ATTRIBUTE)
                    ? contextView.get(CorrelationIdSupport.EXCHANGE_ATTRIBUTE)
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
