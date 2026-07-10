package com.novabank.documento.adapter.out.storage;

import com.novabank.documento.application.exception.DocumentoNotFoundException;
import com.novabank.documento.application.port.out.DocumentoStoragePort;
import com.novabank.documento.domain.model.DocumentoOperacion;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryDocumentoStorageAdapter implements DocumentoStoragePort {

    private final Map<UUID, DocumentoOperacion> documentosPorOperacion = new ConcurrentHashMap<>();

    @Override
    public Mono<URI> generarUrlTemporalDescarga(UUID operacionId) {
        DocumentoOperacion documento = documentosPorOperacion.get(operacionId);
        if (documento == null) {
            return Mono.error(new DocumentoNotFoundException(
                    "No existe documento para la operacion " + operacionId
            ));
        }

        return Mono.just(URI.create("http://localhost:8086/documentos/mock/" + documento.claveObjeto()));
    }

    @Override
    public Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId) {
        return Flux.fromIterable(documentosPorOperacion.values())
                .filter(documento -> documento.cuentaId().equals(cuentaId));
    }

    @Override
    public Mono<Void> eliminarPorOperacion(UUID operacionId) {
        documentosPorOperacion.remove(operacionId);
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> existePorOperacion(UUID operacionId) {
        return Mono.just(documentosPorOperacion.containsKey(operacionId));
    }
}
