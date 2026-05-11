package com.novabank.operacion.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    @Test
    void webClientBuilderEstaAnotadoComoLoadBalanced() throws NoSuchMethodException {
        Method method = WebClientConfig.class.getDeclaredMethod("webClientBuilder");

        assertThat(method.getReturnType()).isEqualTo(WebClient.Builder.class);
        assertThat(AnnotationUtils.findAnnotation(method, LoadBalanced.class)).isNotNull();
    }
}
