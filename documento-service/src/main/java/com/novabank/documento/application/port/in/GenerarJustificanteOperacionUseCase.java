package com.novabank.documento.application.port.in;

import com.novabank.documento.domain.model.DocumentoOperacion;
import reactor.core.publisher.Mono;

public interface GenerarJustificanteOperacionUseCase {

    Mono<DocumentoOperacion> generar(GenerarJustificanteOperacionCommand command);
}
