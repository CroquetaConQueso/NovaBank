package com.novabank.documento.adapter.out.storage;

import com.novabank.documento.application.exception.DocumentoNotFoundException;
import com.novabank.documento.application.exception.DocumentoStorageException;
import com.novabank.documento.application.port.out.DocumentoStoragePort;
import com.novabank.documento.application.port.out.DocumentoUrlTemporal;
import com.novabank.documento.config.S3Properties;
import com.novabank.documento.domain.model.DocumentoId;
import com.novabank.documento.domain.model.DocumentoOperacion;
import com.novabank.documento.domain.model.TipoDocumento;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Component
@ConditionalOnProperty(name = "novabank.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3DocumentoStorageAdapter implements DocumentoStoragePort {

    private static final String META_DOCUMENTO_ID = "documento-id";
    private static final String META_OPERACION_ID = "operacion-id";
    private static final String META_CUENTA_ID = "cuenta-id";
    private static final String META_TIPO_DOCUMENTO = "tipo-documento";
    private static final String META_CREADO_EN = "creado-en";

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public S3DocumentoStorageAdapter(
            S3AsyncClient s3AsyncClient,
            S3Presigner s3Presigner,
            S3Properties properties
    ) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public Mono<DocumentoOperacion> guardar(DocumentoOperacion documento, byte[] contenido) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket())
                .key(documento.claveObjeto())
                .contentType(documento.contentType())
                .metadata(metadata(documento))
                .build();

        return Mono.fromFuture(s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(contenido)))
                .thenReturn(documento)
                .onErrorMap(error -> new DocumentoStorageException(
                        "No se pudo guardar justificante en S3 para la operacion " + documento.operacionId(),
                        unwrap(error)
                ));
    }

    @Override
    public Mono<DocumentoUrlTemporal> generarUrlTemporalDescarga(UUID operacionId) {
        return buscarPorOperacion(operacionId)
                .map(documento -> {
                    Instant expiraEn = Instant.now().plus(properties.getS3().getPresignedUrlTtl());
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(bucket())
                            .key(documento.claveObjeto())
                            .build();
                    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                            .signatureDuration(properties.getS3().getPresignedUrlTtl())
                            .getObjectRequest(getObjectRequest)
                            .build();
                    return new DocumentoUrlTemporal(
                            java.net.URI.create(s3Presigner.presignGetObject(presignRequest).url().toString()),
                            expiraEn
                    );
                });
    }

    @Override
    public Flux<DocumentoOperacion> listarPorCuenta(Long cuentaId) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket())
                .prefix("cuentas/" + cuentaId + "/operaciones/")
                .build();

        return Mono.fromFuture(s3AsyncClient.listObjectsV2(request))
                .flatMapMany(response -> Flux.fromIterable(response.contents()))
                .flatMap(this::toDocumento)
                .onErrorMap(error -> new DocumentoStorageException(
                        "No se pudieron listar justificantes de la cuenta " + cuentaId,
                        unwrap(error)
                ));
    }

    @Override
    public Mono<Void> eliminarPorOperacion(UUID operacionId) {
        return buscarPorOperacion(operacionId)
                .flatMap(documento -> Mono.fromFuture(s3AsyncClient.deleteObject(DeleteObjectRequest.builder()
                                .bucket(bucket())
                                .key(documento.claveObjeto())
                                .build()))
                        .then())
                .onErrorMap(error -> error instanceof DocumentoNotFoundException
                        ? error
                        : new DocumentoStorageException(
                                "No se pudo eliminar justificante de la operacion " + operacionId,
                                unwrap(error)
                        ));
    }

    @Override
    public Mono<Boolean> existePorOperacion(UUID operacionId) {
        return buscarPorOperacion(operacionId)
                .map(ignored -> true)
                .onErrorResume(DocumentoNotFoundException.class, ignored -> Mono.just(false));
    }

    private Mono<DocumentoOperacion> buscarPorOperacion(UUID operacionId) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket())
                .build();

        return Mono.fromFuture(s3AsyncClient.listObjectsV2(request))
                .flatMapMany(response -> Flux.fromIterable(response.contents()))
                .filter(object -> object.key().endsWith("/" + operacionId + ".json"))
                .next()
                .switchIfEmpty(Mono.error(new DocumentoNotFoundException(
                        "No existe documento para la operacion " + operacionId
                )))
                .flatMap(this::toDocumento)
                .onErrorMap(error -> isNotFound(error)
                        ? new DocumentoNotFoundException("No existe documento para la operacion " + operacionId)
                        : error);
    }

    private Mono<DocumentoOperacion> toDocumento(S3Object object) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket())
                .key(object.key())
                .build();

        return Mono.fromFuture(s3AsyncClient.headObject(request))
                .map(response -> toDocumento(object.key(), response));
    }

    private DocumentoOperacion toDocumento(String key, HeadObjectResponse response) {
        Map<String, String> metadata = response.metadata();
        return new DocumentoOperacion(
                new DocumentoId(UUID.fromString(metadata.get(META_DOCUMENTO_ID))),
                UUID.fromString(metadata.get(META_OPERACION_ID)),
                Long.valueOf(metadata.get(META_CUENTA_ID)),
                key,
                TipoDocumento.valueOf(metadata.get(META_TIPO_DOCUMENTO)),
                response.contentType(),
                Instant.parse(metadata.get(META_CREADO_EN))
        );
    }

    private Map<String, String> metadata(DocumentoOperacion documento) {
        return Map.of(
                META_DOCUMENTO_ID, documento.documentoId().value().toString(),
                META_OPERACION_ID, documento.operacionId().toString(),
                META_CUENTA_ID, documento.cuentaId().toString(),
                META_TIPO_DOCUMENTO, documento.tipoDocumento().name(),
                META_CREADO_EN, documento.creadoEn().toString()
        );
    }

    private String bucket() {
        return properties.getS3().getBucketJustificantes();
    }

    private boolean isNotFound(Throwable error) {
        Throwable unwrapped = unwrap(error);
        return unwrapped instanceof NoSuchKeyException
                || (unwrapped instanceof S3Exception s3Exception && s3Exception.statusCode() == 404);
    }

    private Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return error;
    }
}
