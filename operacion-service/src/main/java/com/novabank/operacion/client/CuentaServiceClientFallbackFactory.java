package com.novabank.operacion.client;

import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import com.novabank.operacion.exception.RemoteConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CuentaServiceClientFallbackFactory implements FallbackFactory<CuentaServiceClient> {

    @Override
    public CuentaServiceClient create(Throwable cause) {
        return new CuentaServiceClient() {
            @Override
            public MovimientoResponseDTO depositar(
                    Long id,
                    CuentaOperacionRequestDTO request
            ) {
                throw translate(cause);
            }

            @Override
            public MovimientoResponseDTO retirar(
                    Long id,
                    CuentaOperacionRequestDTO request
            ) {
                throw translate(cause);
            }

            @Override
            public List<MovimientoResponseDTO> transferir(
                    TransferenciaInternaRequestDTO request
            ) {
                throw translate(cause);
            }
        };
    }

    private RuntimeException translate(Throwable cause) {
        if (cause instanceof RemoteResourceNotFoundException exception) {
            return exception;
        }
        if (cause instanceof RemoteValidationException exception) {
            return exception;
        }
        if (cause instanceof RemoteConflictException exception) {
            return exception;
        }
        if (cause instanceof RemoteServiceException exception) {
            return exception;
        }
        return new RemoteServiceException("cuenta-service no esta disponible", cause);
    }
}
