package com.novabank.documento.application.port.in;

import reactor.core.publisher.Flux;

public interface ListarDocumentosCuentaUseCase {

    Flux<DocumentoResumenResult> listarPorCuenta(Long cuentaId);
}
