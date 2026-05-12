package com.novabank.gateway.config;

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
    void webClientBuilderParaAuthServerUsaLoadBalancer() throws NoSuchMethodException {
        Method method = WebClientConfig.class.getDeclaredMethod("loadBalancedWebClientBuilder");

        assertThat(method.isAnnotationPresent(LoadBalanced.class)).isTrue();
    }

    @Test
    void creaWebClientBuilderReactivo() {
        WebClient.Builder builder = new WebClientConfig().loadBalancedWebClientBuilder();

        assertThat(builder).isNotNull();
    }

    @Test
    void aplicaCustomizersDeSpringBootParaObservabilidadWebClient() {
        AtomicBoolean customizerAplicado = new AtomicBoolean(false);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("tracingCustomizer", (WebClientCustomizer) builder -> customizerAplicado.set(true));

        new WebClientConfig(beanFactory.getBeanProvider(WebClientCustomizer.class)).loadBalancedWebClientBuilder();

        assertThat(customizerAplicado).isTrue();
    }
}
