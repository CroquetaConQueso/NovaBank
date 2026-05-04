package com.novabank.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    @Test
    void webClientBuilderParaAuthServerUsaLoadBalancer() throws NoSuchMethodException {
        Method method = WebClientConfig.class.getDeclaredMethod("loadBalancedWebClientBuilder");

        assertThat(method.isAnnotationPresent(LoadBalanced.class)).isTrue();
    }

    @Test
    void creaWebClientBuilderReactivo() {
        WebClient.Builder builder = new WebClientConfig().loadBalancedWebClientBuilder();

        assertThat(builder).isNotNull();
    }
}
