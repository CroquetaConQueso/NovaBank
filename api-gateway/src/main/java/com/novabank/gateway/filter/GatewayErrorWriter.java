package com.novabank.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.gateway.dto.ErrorResponseDTO;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayErrorWriter {

    private final ObjectMapper objectMapper;

    public GatewayErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.findAndRegisterModules();
    }

    public Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatus status,
            String code,
            String message,
            String correlationId
    ) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");

        byte[] bytes = serialize(ErrorResponseDTO.of(code, message, correlationId));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] serialize(ErrorResponseDTO error) {
        try {
            return objectMapper.writeValueAsBytes(error);
        } catch (JsonProcessingException ex) {
            return ("{\"code\":\"INTERNAL_GATEWAY_ERROR\",\"message\":\"No se pudo serializar el error\"}")
                    .getBytes();
        }
    }
}
