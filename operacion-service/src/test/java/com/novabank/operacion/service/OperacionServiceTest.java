package com.novabank.operacion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.IdempotencyConflictException;
import com.novabank.operacion.exception.RemoteValidationException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.model.EstadoOperacion;
import com.novabank.operacion.model.OperacionIdempotente;
import com.novabank.operacion.model.TipoOperacion;
import com.novabank.operacion.repository.OperacionIdempotenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionServiceTest {

    private CuentaServiceClient cuentaServiceClient;
    private OperacionIdempotenteRepository repository;
    private OperacionService service;

    @BeforeEach
    void setUp() {
        cuentaServiceClient = mock(CuentaServiceClient.class);
        repository = mock(OperacionIdempotenteRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new OperacionService(cuentaServiceClient, repository, objectMapper);
    }

    @Test
    void depositoCorrectoRegistraIdempotenciaYDelegaEnCuentaService() {
        when(repository.findByIdempotencyKey("dep-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(movimiento("DEPOSITO"));

        OperacionResponseDTO response = service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.00")),
                "dep-1",
                "corr-1"
        );

        assertThat(response.estado()).isEqualTo(EstadoOperacion.COMPLETADA);
        assertThat(response.movimientos()).hasSize(1);
        verify(cuentaServiceClient).depositar(eq(10L), any(CuentaOperacionRequestDTO.class));
        verify(repository).saveAndFlush(any(OperacionIdempotente.class));
    }

    @Test
    void retiroCorrectoDelegaEnCuentaService() {
        when(repository.findByIdempotencyKey("ret-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(movimiento("RETIRO"));

        OperacionResponseDTO response = service.retirar(
                new OperacionRequestDTO(10L, new BigDecimal("25.00")),
                "ret-1",
                "corr-1"
        );

        assertThat(response.tipoOperacion()).isEqualTo(TipoOperacion.RETIRO);
        verify(cuentaServiceClient).retirar(eq(10L), any(CuentaOperacionRequestDTO.class));
    }

    @Test
    void transferenciaCorrectaUsaEndpointInternoUnico() {
        when(repository.findByIdempotencyKey("tra-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaServiceClient.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(List.of(movimiento("TRANSFERENCIA_SALIENTE"), movimiento("TRANSFERENCIA_ENTRANTE")));

        OperacionResponseDTO response = service.transferir(
                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("25.00")),
                "tra-1",
                "corr-1"
        );

        assertThat(response.movimientos()).hasSize(2);
        verify(cuentaServiceClient).transferir(any(TransferenciaInternaRequestDTO.class));
        verify(cuentaServiceClient, never()).retirar(any(), any());
        verify(cuentaServiceClient, never()).depositar(any(), any());
    }

    @Test
    void mismaKeyYMayorRequestDevuelveRespuestaGuardadaSinRepetirOperacion() {
        when(repository.findByIdempotencyKey("dep-2")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(movimiento("DEPOSITO"));

        service.depositar(new OperacionRequestDTO(10L, new BigDecimal("50.00")), "dep-2", "corr-1");

        ArgumentCaptor<OperacionIdempotente> captor = ArgumentCaptor.forClass(OperacionIdempotente.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        OperacionIdempotente completed = captor.getValue();

        when(repository.findByIdempotencyKey("dep-2")).thenReturn(Optional.of(completed));

        OperacionResponseDTO replay = service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.0")),
                "dep-2",
                "corr-2"
        );

        assertThat(replay.estado()).isEqualTo(EstadoOperacion.COMPLETADA);
        verify(cuentaServiceClient, org.mockito.Mockito.times(1))
                .depositar(eq(10L), any(CuentaOperacionRequestDTO.class));
    }

    @Test
    void mismaKeyConRequestDistintoDevuelveConflicto() {
        OperacionIdempotente existing = new OperacionIdempotente();
        existing.setIdempotencyKey("key-conflict");
        existing.setRequestHash("hash-distinto");
        existing.setTipoOperacion(TipoOperacion.DEPOSITO);
        existing.setEstado(EstadoOperacion.COMPLETADA);
        existing.setImporte(new BigDecimal("10.00"));
        existing.setCuentaOrigen(10L);

        when(repository.findByIdempotencyKey("key-conflict")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.00")),
                "key-conflict",
                "corr-1"
        ))
                .isInstanceOf(IdempotencyConflictException.class);

        verify(cuentaServiceClient, never()).depositar(any(), any());
    }

    @Test
    void errorRemotoQuedaPersistidoComoFallido() {
        when(repository.findByIdempotencyKey("ret-2")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(OperacionIdempotente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new RemoteValidationException("Saldo insuficiente"));

        assertThatThrownBy(() -> service.retirar(
                new OperacionRequestDTO(10L, new BigDecimal("999.00")),
                "ret-2",
                "corr-1"
        ))
                .isInstanceOf(RemoteValidationException.class);

        ArgumentCaptor<OperacionIdempotente> captor = ArgumentCaptor.forClass(OperacionIdempotente.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoOperacion.FALLIDA);
        assertThat(captor.getValue().getErrorMessage()).contains("Saldo insuficiente");
    }

    @Test
    void idempotencyKeyEsObligatoria() {
        assertThatThrownBy(() -> service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.00")),
                " ",
                "corr-1"
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La cabecera Idempotency-Key es obligatoria");
    }

    private MovimientoResponseDTO movimiento(String tipo) {
        return new MovimientoResponseDTO(
                1L,
                10L,
                "ES91210000000000000001",
                tipo,
                new BigDecimal("50.00"),
                LocalDateTime.now()
        );
    }
}
