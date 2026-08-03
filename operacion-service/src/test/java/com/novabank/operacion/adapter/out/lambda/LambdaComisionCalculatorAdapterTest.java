package com.novabank.operacion.adapter.out.lambda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.application.exception.ComisionNoDisponibleException;
import com.novabank.operacion.application.usecase.ComisionCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LambdaComisionCalculatorAdapterTest {

    private final LambdaAsyncClient lambdaAsyncClient = mock(LambdaAsyncClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final LambdaComisionCalculatorAdapter adapter = new LambdaComisionCalculatorAdapter(
            lambdaAsyncClient,
            objectMapper,
            "novabank-comision",
            Duration.ofSeconds(1)
    );

    @Test
    void convierteRequestResponseCorrectamente() throws Exception {
        InvokeResponse response = InvokeResponse.builder()
                .statusCode(200)
                .payload(SdkBytes.fromUtf8String("""
                        {
                          "comision": 16.00,
                          "tasaAplicada": 0.0160,
                          "paisDestino": "US",
                          "tipoCliente": "EMPRESA"
                        }
                        """))
                .build();
        when(lambdaAsyncClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(adapter.calcularComision(new ComisionCommand(
                        new BigDecimal("1000.00"),
                        "US",
                        "EMPRESA"
                )))
                .assertNext(comision -> {
                    assertThat(comision.comision()).isEqualByComparingTo("16.00");
                    assertThat(comision.tasaAplicada()).isEqualByComparingTo("0.0160");
                    assertThat(comision.paisDestino()).isEqualTo("US");
                    assertThat(comision.tipoCliente()).isEqualTo("EMPRESA");
                })
                .verifyComplete();

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        org.mockito.Mockito.verify(lambdaAsyncClient).invoke(captor.capture());
        assertThat(captor.getValue().functionName()).isEqualTo("novabank-comision");

        JsonNode requestJson = objectMapper.readTree(captor.getValue().payload().asUtf8String());
        assertThat(requestJson.get("importeEuros").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(requestJson.get("paisDestino").asText()).isEqualTo("US");
        assertThat(requestJson.get("tipoCliente").asText()).isEqualTo("EMPRESA");
    }

    @Test
    void mapeaErrorTecnicoAComisionNoDisponible() {
        when(lambdaAsyncClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("LocalStack caido")));

        StepVerifier.create(adapter.calcularComision(new ComisionCommand(
                        new BigDecimal("1000.00"),
                        "US",
                        "EMPRESA"
                )))
                .expectError(ComisionNoDisponibleException.class)
                .verify();
    }

    @Test
    void mapeaFunctionErrorAComisionNoDisponible() {
        InvokeResponse response = InvokeResponse.builder()
                .statusCode(200)
                .functionError("Unhandled")
                .payload(SdkBytes.fromUtf8String("{}"))
                .build();
        when(lambdaAsyncClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(adapter.calcularComision(new ComisionCommand(
                        new BigDecimal("1000.00"),
                        "US",
                        "EMPRESA"
                )))
                .expectError(ComisionNoDisponibleException.class)
                .verify();
    }
}
