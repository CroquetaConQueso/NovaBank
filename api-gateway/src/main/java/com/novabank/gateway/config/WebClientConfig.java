package com.novabank.gateway.config;

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

import com.novabank.gateway.tracing.CorrelationIdSupport;

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

    /**
     * Habilita la resolucion de nombres lb:// para que WebClient pueda llamar a
     * servicios registrados en Eureka.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        WebClient.Builder builder = WebClient.builder()
                .filter(correlationIdFilter());
        if (webClientCustomizers != null) {
            webClientCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        }
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
