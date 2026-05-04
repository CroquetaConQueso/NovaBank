package com.novabank.cuenta.client;

import com.novabank.cuenta.exception.RemoteServiceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ClienteServiceClientFallbackFactory implements FallbackFactory<ClienteServiceClient> {

    @Override
    public ClienteServiceClient create(Throwable cause) {
        return id -> {
            throw translate(cause);
        };
    }

    private RuntimeException translate(Throwable cause) {
        if (cause instanceof ResourceNotFoundException exception) {
            return exception;
        }
        if (cause instanceof RemoteServiceException exception) {
            return exception;
        }
        return new RemoteServiceException("cliente-service no esta disponible", cause);
    }
}
