package com.novabank.documento.adapter.out.storage;

import com.novabank.documento.config.S3Properties;
import com.novabank.documento.domain.model.DocumentoId;
import com.novabank.documento.domain.model.DocumentoOperacion;
import com.novabank.documento.domain.model.TipoDocumento;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3DocumentoStorageAdapterTest {

    @Test
    void guardarSubeObjetoConMetadata() {
        S3AsyncClient s3AsyncClient = mock(S3AsyncClient.class);
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));
        S3DocumentoStorageAdapter adapter = adapter(s3AsyncClient);
        UUID operationId = UUID.randomUUID();
        DocumentoOperacion documento = documento(operationId, 10L);

        StepVerifier.create(adapter.guardar(documento, "{\"ok\":true}".getBytes()))
                .expectNext(documento)
                .verifyComplete();

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3AsyncClient).putObject(captor.capture(), any(AsyncRequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("novabank-justificantes");
        assertThat(captor.getValue().key()).isEqualTo(documento.claveObjeto());
        assertThat(captor.getValue().metadata()).containsEntry("operacion-id", operationId.toString());
        assertThat(captor.getValue().metadata()).containsEntry("cuenta-id", "10");
    }

    @Test
    void listarPorCuentaReconstruyeDocumentoDesdeS3() {
        S3AsyncClient s3AsyncClient = mock(S3AsyncClient.class);
        UUID operationId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        String key = "cuentas/10/operaciones/2026/07/" + operationId + ".json";
        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(CompletableFuture.completedFuture(ListObjectsV2Response.builder()
                        .contents(S3Object.builder().key(key).build())
                        .build()));
        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                        .contentType("application/json")
                        .metadata(Map.of(
                                "documento-id", documentoId.toString(),
                                "operacion-id", operationId.toString(),
                                "cuenta-id", "10",
                                "tipo-documento", "JUSTIFICANTE_OPERACION",
                                "creado-en", "2026-07-08T08:00:00Z"
                        ))
                        .build()));
        S3DocumentoStorageAdapter adapter = adapter(s3AsyncClient);

        StepVerifier.create(adapter.listarPorCuenta(10L))
                .assertNext(documento -> {
                    assertThat(documento.documentoId().value()).isEqualTo(documentoId);
                    assertThat(documento.operacionId()).isEqualTo(operationId);
                    assertThat(documento.cuentaId()).isEqualTo(10L);
                    assertThat(documento.claveObjeto()).isEqualTo(key);
                })
                .verifyComplete();
    }

    private S3DocumentoStorageAdapter adapter(S3AsyncClient s3AsyncClient) {
        S3Properties properties = new S3Properties();
        properties.getS3().setBucketJustificantes("novabank-justificantes");
        properties.getS3().setPresignedUrlTtl(Duration.ofMinutes(15));
        return new S3DocumentoStorageAdapter(s3AsyncClient, mock(S3Presigner.class), properties);
    }

    private DocumentoOperacion documento(UUID operationId, Long cuentaId) {
        return new DocumentoOperacion(
                DocumentoId.nuevo(),
                operationId,
                cuentaId,
                "cuentas/" + cuentaId + "/operaciones/2026/07/" + operationId + ".json",
                TipoDocumento.JUSTIFICANTE_OPERACION,
                "application/json",
                Instant.parse("2026-07-08T08:00:00Z")
        );
    }
}
