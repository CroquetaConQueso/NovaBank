package com.novabank.cuenta.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    @Test
    void webClientBuilderUsaLoadBalancedParaResolverClienteServicePorEureka() throws NoSuchMethodException {
        Method method = WebClientConfig.class.getDeclaredMethod("webClientBuilder");

        assertThat(method.isAnnotationPresent(LoadBalanced.class)).isTrue();
        assertThat(new WebClientConfig().webClientBuilder()).isInstanceOf(WebClient.Builder.class);
    }
}
