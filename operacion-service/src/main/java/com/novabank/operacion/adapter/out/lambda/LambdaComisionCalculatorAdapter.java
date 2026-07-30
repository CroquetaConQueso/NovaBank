package com.novabank.operacion.adapter.out.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.application.exception.ComisionNoDisponibleException;
import com.novabank.operacion.application.port.out.ComisionCalculatorPort;
import com.novabank.operacion.application.usecase.ComisionCalculada;
import com.novabank.operacion.application.usecase.ComisionCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.time.Duration;

@Component
public class LambdaComisionCalculatorAdapter implements ComisionCalculatorPort {

    private static final String ERROR_COMISION_NO_DISPONIBLE =
            "No se pudo calcular la comision de la transferencia internacional";

    private final LambdaAsyncClient lambdaAsyncClient;
    private final ObjectMapper objectMapper;
    private final String functionName;
    private final Duration timeout;

    public LambdaComisionCalculatorAdapter(
            LambdaAsyncClient lambdaAsyncClient,
            ObjectMapper objectMapper,
            @Value("${novabank.lambda.comision-function-name:novabank-comision}") String functionName,
            @Value("${novabank.lambda.comision-timeout:3s}") Duration timeout
    ) {
        this.lambdaAsyncClient = lambdaAsyncClient;
        this.objectMapper = objectMapper;
        this.functionName = functionName;
        this.timeout = timeout;
    }

    @Override
    public Mono<ComisionCalculada> calcularComision(ComisionCommand command) {
        return Mono.defer(() -> {
            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromByteArray(toJson(command)))
                    .build();

            return Mono.fromFuture(lambdaAsyncClient.invoke(request))
                    .timeout(timeout)
                    .flatMap(this::toComisionCalculada)
                    .onErrorMap(this::toComisionNoDisponible);
        });
    }

    private byte[] toJson(ComisionCommand command) {
        try {
            return objectMapper.writeValueAsBytes(new ComisionLambdaRequest(
                    command.importeEuros(),
                    command.paisDestino(),
                    command.tipoCliente()
            ));
        } catch (JsonProcessingException ex) {
            throw new ComisionNoDisponibleException(ERROR_COMISION_NO_DISPONIBLE, ex);
        }
    }

    private Mono<ComisionCalculada> toComisionCalculada(InvokeResponse response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300 || hasText(response.functionError())) {
            return Mono.error(new ComisionNoDisponibleException(ERROR_COMISION_NO_DISPONIBLE));
        }

        try {
            ComisionLambdaResponse lambdaResponse = objectMapper.readValue(
                    response.payload().asByteArray(),
                    ComisionLambdaResponse.class
            );

            if (lambdaResponse.comision() == null || lambdaResponse.tasaAplicada() == null) {
                return Mono.error(new ComisionNoDisponibleException(ERROR_COMISION_NO_DISPONIBLE));
            }

            return Mono.just(new ComisionCalculada(
                    lambdaResponse.comision(),
                    lambdaResponse.tasaAplicada(),
                    lambdaResponse.paisDestino(),
                    lambdaResponse.tipoCliente()
            ));
        } catch (Exception ex) {
            return Mono.error(new ComisionNoDisponibleException(ERROR_COMISION_NO_DISPONIBLE, ex));
        }
    }

    private Throwable toComisionNoDisponible(Throwable error) {
        if (error instanceof ComisionNoDisponibleException) {
            return error;
        }
        return new ComisionNoDisponibleException(ERROR_COMISION_NO_DISPONIBLE, error);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
