package com.novabank.operacion.client;

import com.novabank.operacion.exception.RemoteConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public class CuentaServiceFeignConfig {

    @Bean
    HttpMessageConverters feignHttpMessageConverters() {
        return new HttpMessageConverters(new MappingJackson2HttpMessageConverter());
    }

    @Bean
    ErrorDecoder cuentaServiceErrorDecoder() {
        return new CuentaServiceErrorDecoder();
    }

    private static class CuentaServiceErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            return switch (response.status()) {
                case 400 -> new RemoteValidationException("cuenta-service rechazo la peticion interna");
                case 404 -> new RemoteResourceNotFoundException("La cuenta indicada no existe");
                case 409 -> new RemoteConflictException("La operacion no pudo completarse por conflicto de concurrencia");
                case 422 -> new RemoteValidationException("La operacion fue rechazada por cuenta-service");
                case 503 -> new RemoteServiceException("cuenta-service no esta disponible");
                default -> response.status() >= 500
                        ? new RemoteServiceException("cuenta-service no esta disponible")
                        : defaultDecoder.decode(methodKey, response);
            };
        }
    }
}
