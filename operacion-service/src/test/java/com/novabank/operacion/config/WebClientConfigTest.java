package com.novabank.operacion.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    @Test
    void webClientBuilderEstaAnotadoComoLoadBalanced() throws Exception {
        Method method = WebClientConfig.class.getDeclaredMethod("webClientBuilder");

        assertThat(method.isAnnotationPresent(LoadBalanced.class)).isTrue();
        assertThat(new WebClientConfig().webClientBuilder()).isInstanceOf(WebClient.Builder.class);
    }

    @Test
    void aplicaCustomizersDeSpringBootParaObservabilidadWebClient() {
        AtomicBoolean customizerAplicado = new AtomicBoolean(false);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("tracingCustomizer", (WebClientCustomizer) builder -> customizerAplicado.set(true));

        new WebClientConfig(beanFactory.getBeanProvider(WebClientCustomizer.class)).webClientBuilder();

        assertThat(customizerAplicado).isTrue();
    }
}
