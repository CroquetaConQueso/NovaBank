package com.novabank.operacion.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
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

    private final ObjectProvider<WebClientCustomizer> webClientCustomizers;

    public WebClientConfig() {
        this.webClientCustomizers = null;
    }

    @Autowired
    public WebClientConfig(ObjectProvider<WebClientCustomizer> webClientCustomizers) {
        this.webClientCustomizers = webClientCustomizers;
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder builder = WebClient.builder()
                .filter(correlationIdFilter());
        if (webClientCustomizers != null) {
            webClientCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        }
        return builder;
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
