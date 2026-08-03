package com.novabank.documento.adapter.in.web;

import com.novabank.documento.application.port.in.DocumentoResumenResult;
import com.novabank.documento.application.port.in.EliminarDocumentoOperacionUseCase;
import com.novabank.documento.application.port.in.GenerarUrlDescargaDocumentoUseCase;
import com.novabank.documento.application.port.in.ListarDocumentosCuentaUseCase;
import com.novabank.documento.application.port.in.UrlDescargaDocumentoResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
@Tag(name = "Documentos", description = "Justificantes de operaciones bancarias")
public class DocumentoController {

    private final GenerarUrlDescargaDocumentoUseCase generarUrlDescargaDocumentoUseCase;
    private final ListarDocumentosCuentaUseCase listarDocumentosCuentaUseCase;
    private final EliminarDocumentoOperacionUseCase eliminarDocumentoOperacionUseCase;

    public DocumentoController(
            GenerarUrlDescargaDocumentoUseCase generarUrlDescargaDocumentoUseCase,
            ListarDocumentosCuentaUseCase listarDocumentosCuentaUseCase,
            EliminarDocumentoOperacionUseCase eliminarDocumentoOperacionUseCase
    ) {
        this.generarUrlDescargaDocumentoUseCase = generarUrlDescargaDocumentoUseCase;
        this.listarDocumentosCuentaUseCase = listarDocumentosCuentaUseCase;
        this.eliminarDocumentoOperacionUseCase = eliminarDocumentoOperacionUseCase;
    }

    @GetMapping("/operaciones/{operacionId}/url")
    @Operation(
            summary = "Generar URL temporal de descarga",
            description = "Devuelve una URL temporal para descargar el justificante asociado a una operacion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL generada correctamente"),
            @ApiResponse(responseCode = "400", description = "Identificador invalido"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
            @ApiResponse(responseCode = "503", description = "Almacenamiento no disponible")
    })
    public Mono<DocumentoUrlResponseDTO> generarUrlDescarga(@PathVariable UUID operacionId) {
        return generarUrlDescargaDocumentoUseCase.generarUrlDescarga(operacionId)
                .map(this::toUrlResponse);
    }

    @GetMapping("/cuentas/{cuentaId}")
    @Operation(
            summary = "Listar documentos de una cuenta",
            description = "Devuelve los justificantes conocidos para una cuenta bancaria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documentos obtenidos correctamente"),
            @ApiResponse(responseCode = "400", description = "Identificador invalido"),
            @ApiResponse(responseCode = "503", description = "Almacenamiento no disponible")
    })
    public Flux<DocumentoResumenResponseDTO> listarPorCuenta(@PathVariable Long cuentaId) {
        return listarDocumentosCuentaUseCase.listarPorCuenta(cuentaId)
                .map(this::toResumenResponse);
    }

    @DeleteMapping("/operaciones/{operacionId}")
    @Operation(
            summary = "Eliminar documento de operacion",
            description = "Elimina el justificante asociado a una operacion si existe."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Documento eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "Identificador invalido"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
            @ApiResponse(responseCode = "503", description = "Almacenamiento no disponible")
    })
    public Mono<ResponseEntity<Void>> eliminarPorOperacion(@PathVariable UUID operacionId) {
        return eliminarDocumentoOperacionUseCase.eliminarPorOperacion(operacionId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    private DocumentoUrlResponseDTO toUrlResponse(UrlDescargaDocumentoResult result) {
        return new DocumentoUrlResponseDTO(result.operacionId(), result.url(), result.expiraEn());
    }

    private DocumentoResumenResponseDTO toResumenResponse(DocumentoResumenResult result) {
        return new DocumentoResumenResponseDTO(
                result.documentoId(),
                result.operacionId(),
                result.cuentaId(),
                result.tipoDocumento().name(),
                result.contentType(),
                result.creadoEn()
        );
    }
}
