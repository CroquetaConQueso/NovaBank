package com.novabank.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.novabank.gateway.tracing.CorrelationIdSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;

@Configuration
public class WebClientConfig {

    private final java.util.List<WebClientCustomizer> customizers;

    public WebClientConfig(ObjectProvider<WebClientCustomizer> customizers) {
        this.customizers = customizers.orderedStream().toList();
    }

    public WebClientConfig() {
        this.customizers = java.util.List.of();
    }

    /**
     * Habilita la resolucion de nombres lb:// para que WebClient pueda llamar a
     * servicios registrados en Eureka.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        WebClient.Builder builder = WebClient.builder()
                .filter(correlationIdFilter());
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder;
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
