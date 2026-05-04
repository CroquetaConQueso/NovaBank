package com.novabank.cuenta.client;

import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class ClienteServiceFeignConfig {

    @Bean
    ErrorDecoder clienteServiceErrorDecoder() {
        return new ClienteServiceErrorDecoder();
    }

    private static class ClienteServiceErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            if (response.status() == 404) {
                return new ResourceNotFoundException("No existe ningun cliente con el id indicado");
            }
            if (response.status() >= 500) {
                return new RemoteServiceException("cliente-service no esta disponible");
            }
            return defaultDecoder.decode(methodKey, response);
        }
    }
}
