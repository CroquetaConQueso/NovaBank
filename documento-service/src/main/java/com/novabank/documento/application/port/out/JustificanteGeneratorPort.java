package com.novabank.documento.application.port.out;

import com.novabank.documento.application.port.in.GenerarJustificanteOperacionCommand;
import reactor.core.publisher.Mono;

public interface JustificanteGeneratorPort {

    Mono<GeneratedJustificante> generar(GenerarJustificanteOperacionCommand command);
}
