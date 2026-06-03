package com.novabank.cliente.event;

import com.novabank.cliente.exception.EventoNoPublicadoException;
import com.novabank.cliente.model.Cliente;
import com.novabank.cliente.tracing.CorrelationIdSupport;
import com.novabank.events.cliente.ClienteRegistradoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteEventPublisherTest {

    private final StreamBridge streamBridge = mock(StreamBridge.class);
    private final ClienteEventPublisher publisher = new ClienteEventPublisher(streamBridge);

    @Test
    void publicarClienteRegistradoCompletaCuandoStreamBridgeDevuelveTrue() {
        Cliente cliente = clienteGuardado();
        when(streamBridge.send(eq(ClienteEventPublisher.CLIENTE_REGISTRADO_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        StepVerifier.create(publisher.publicarClienteRegistrado(cliente)
                        .contextWrite(context -> context.put(
                                CorrelationIdSupport.CONTEXT_KEY,
                                "33333333-3333-3333-3333-333333333333"
                        )))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<ClienteRegistradoEvent>> captor =
                ArgumentCaptor.forClass((Class<Message<ClienteRegistradoEvent>>) (Class<?>) Message.class);
        verify(streamBridge).send(eq(ClienteEventPublisher.CLIENTE_REGISTRADO_BINDING), captor.capture());

        ClienteRegistradoEvent event = captor.getValue().getPayload();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        assertThat(event.clienteId()).isEqualTo(1L);
        assertThat(event.nombre()).isEqualTo("Ana");
        assertThat(event.email()).isEqualTo("ana@example.com");
    }

    @Test
    void publicarClienteRegistradoEmiteErrorCuandoStreamBridgeDevuelveFalse() {
        when(streamBridge.send(eq(ClienteEventPublisher.CLIENTE_REGISTRADO_BINDING), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        StepVerifier.create(publisher.publicarClienteRegistrado(clienteGuardado()))
                .expectError(EventoNoPublicadoException.class)
                .verify();
    }

    private Cliente clienteGuardado() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ana");
        cliente.setApellidos("Garcia");
        cliente.setDni("12345678Z");
        cliente.setEmail("ana@example.com");
        cliente.setTelefono("600111222");
        return cliente;
    }
}
