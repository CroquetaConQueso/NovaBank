package com.novabank.operacion.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    @Test
    void webClientBuilderEstaAnotadoComoLoadBalanced() throws Exception {
        Method method = WebClientConfig.class.getDeclaredMethod("webClientBuilder");

        assertThat(method.isAnnotationPresent(LoadBalanced.class)).isTrue();
        assertThat(new WebClientConfig().webClientBuilder()).isInstanceOf(WebClient.Builder.class);
    }

    @Test
    void aplicaCustomizersDeWebClientParaInstrumentacionDeTracing() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("tracingCustomizer", (WebClientCustomizer) builder ->
                builder.defaultHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"));
        AtomicReference<String> traceparent = new AtomicReference<>();

        WebClient client = new WebClientConfig(beanFactory.getBeanProvider(WebClientCustomizer.class))
                .webClientBuilder()
                .exchangeFunction(request -> {
                    traceparent.set(request.headers().getFirst("traceparent"));
                    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
                .build();

        StepVerifier.create(client.get().uri("http://example.test").retrieve().toBodilessEntity())
                .expectNextCount(1)
                .verifyComplete();

        assertThat(traceparent.get()).isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
    }
}
