package com.novabank.documento.application.usecase;

import com.novabank.documento.application.exception.DocumentoNotFoundException;
import com.novabank.documento.application.port.in.DocumentoResumenResult;
import com.novabank.documento.application.port.in.EliminarDocumentoOperacionUseCase;
import com.novabank.documento.application.port.in.GenerarUrlDescargaDocumentoUseCase;
import com.novabank.documento.application.port.in.ListarDocumentosCuentaUseCase;
import com.novabank.documento.application.port.in.UrlDescargaDocumentoResult;
import com.novabank.documento.application.port.out.DocumentoUrlTemporal;
import com.novabank.documento.application.port.out.DocumentoStoragePort;
import com.novabank.documento.domain.model.DocumentoOperacion;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class DocumentoUseCaseService implements
        GenerarUrlDescargaDocumentoUseCase,
        ListarDocumentosCuentaUseCase,
        EliminarDocumentoOperacionUseCase {

    private final DocumentoStoragePort storagePort;

    public DocumentoUseCaseService(DocumentoStoragePort storagePort) {
        this.storagePort = storagePort;
    }

    @Override
    public Mono<UrlDescargaDocumentoResult> generarUrlDescarga(UUID operacionId) {
        return validarOperacionId(operacionId)
                .then(storagePort.generarUrlTemporalDescarga(operacionId))
                .map(urlTemporal -> toUrlResult(operacionId, urlTemporal));
    }

    @Override
    public Flux<DocumentoResumenResult> listarPorCuenta(Long cuentaId) {
        if (cuentaId == null || cuentaId <= 0) {
            return Flux.error(new IllegalArgumentException("cuentaId debe ser mayor que cero"));
        }

        return storagePort.listarPorCuenta(cuentaId)
                .map(this::toResumen);
    }

    @Override
    public Mono<Void> eliminarPorOperacion(UUID operacionId) {
        return validarOperacionId(operacionId)
                .then(storagePort.existePorOperacion(operacionId))
                .flatMap(existe -> existe
                        ? storagePort.eliminarPorOperacion(operacionId)
                        : Mono.error(new DocumentoNotFoundException(
                                "No existe documento para la operacion " + operacionId
                        )));
    }

    private Mono<Void> validarOperacionId(UUID operacionId) {
        return operacionId == null
                ? Mono.error(new IllegalArgumentException("operacionId es obligatorio"))
                : Mono.empty();
    }

    private DocumentoResumenResult toResumen(DocumentoOperacion documento) {
        return new DocumentoResumenResult(
                documento.documentoId().value(),
                documento.operacionId(),
                documento.cuentaId(),
                documento.tipoDocumento(),
                documento.contentType(),
                documento.creadoEn()
        );
    }

    private UrlDescargaDocumentoResult toUrlResult(UUID operacionId, DocumentoUrlTemporal urlTemporal) {
        return new UrlDescargaDocumentoResult(operacionId, urlTemporal.url(), urlTemporal.expiraEn());
    }
}
