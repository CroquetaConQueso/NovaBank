package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.AplicarMovimientoRequestDTO;
import com.novabank.cuenta.application.port.out.MovimientoRegistradoPublisherPort;
import com.novabank.cuenta.exception.IdempotencyConflictException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.EstadoOperacionIdempotente;
import com.novabank.cuenta.model.OperacionIdempotente;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.repository.OperacionIdempotenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CuentaMovimientoAtomicoConcurrencyTest {

    private CuentaRepository cuentaRepository;
    private OperacionIdempotenteRepository operacionIdempotenteRepository;
    private MovimientoRegistradoPublisherPort movimientoRegistradoEventPublisher;
    private SaldoBajoAlertService saldoBajoAlertService;
    private CuentaMovimientoAtomicoService service;

    @BeforeEach
    void setUp() {
        cuentaRepository = mock(CuentaRepository.class);
        operacionIdempotenteRepository = mock(OperacionIdempotenteRepository.class);
        movimientoRegistradoEventPublisher = mock(MovimientoRegistradoPublisherPort.class);
        saldoBajoAlertService = mock(SaldoBajoAlertService.class);
        service = new CuentaMovimientoAtomicoService(
                cuentaRepository,
                operacionIdempotenteRepository,
                new CuentaMapper(),
                movimientoRegistradoEventPublisher,
                saldoBajoAlertService
        );
    }

    @Test
    void colisionUniqueConMismoHashDevuelveRespuestaIdempotenteControlada() {
        AplicarMovimientoRequestDTO request = request("op-race-1", "25.00", "Transferencia interna");
        OperacionIdempotente existente = operacion(
                request.operationId(),
                hash(request),
                EstadoOperacionIdempotente.COMPLETED
        );
        when(operacionIdempotenteRepository.findByOperationId("op-race-1"))
                .thenReturn(Mono.empty(), Mono.just(existente));
        when(operacionIdempotenteRepository.insertProcessingIfAbsent("op-race-1", hash(request)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("duplicate operation_id")));
        when(cuentaRepository.findById(1L)).thenReturn(Mono.just(cuenta(1L, "75.00")));
        when(cuentaRepository.findById(2L)).thenReturn(Mono.just(cuenta(2L, "125.00")));

        StepVerifier.create(service.aplicarMovimiento(request))
                .assertNext(response -> {
                    assertThat(response.operationId()).isEqualTo("op-race-1");
                    assertThat(response.mensaje()).isEqualTo("Operacion ya aplicada previamente");
                    assertThat(response.cuentaOrigen().saldo()).isEqualByComparingTo("75.00");
                    assertThat(response.cuentaDestino().saldo()).isEqualByComparingTo("125.00");
                })
                .verifyComplete();

        verify(movimientoRegistradoEventPublisher, never()).publicar(org.mockito.ArgumentMatchers.any());
        verify(saldoBajoAlertService, never()).evaluarYPublicar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void colisionUniqueConHashDistintoDevuelveConflictoControlado() {
        AplicarMovimientoRequestDTO request = request("op-race-2", "25.00", "Transferencia interna");
        OperacionIdempotente existente = operacion(
                request.operationId(),
                "hash-distinto",
                EstadoOperacionIdempotente.COMPLETED
        );
        when(operacionIdempotenteRepository.findByOperationId("op-race-2"))
                .thenReturn(Mono.empty(), Mono.just(existente));
        when(operacionIdempotenteRepository.insertProcessingIfAbsent("op-race-2", hash(request)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("duplicate operation_id")));

        StepVerifier.create(service.aplicarMovimiento(request))
                .expectError(IdempotencyConflictException.class)
                .verify();

        verify(cuentaRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    private AplicarMovimientoRequestDTO request(String operationId, String monto, String concepto) {
        return new AplicarMovimientoRequestDTO(
                operationId,
                1L,
                2L,
                new BigDecimal(monto),
                concepto
        );
    }

    private OperacionIdempotente operacion(
            String operationId,
            String requestHash,
            EstadoOperacionIdempotente estado
    ) {
        return OperacionIdempotente.builder()
                .id(1L)
                .operationId(operationId)
                .requestHash(requestHash)
                .estado(estado)
                .build();
    }

    private Cuenta cuenta(Long id, String saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(id);
        cuenta.setClienteId(1L);
        cuenta.setNumeroCuenta("ES9121000000000000000" + id);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.prepararParaCreacion();
        return cuenta;
    }

    private String hash(AplicarMovimientoRequestDTO request) {
        String contenido = String.join("|",
                request.operationId().trim(),
                request.cuentaOrigenId().toString(),
                request.cuentaDestinoId().toString(),
                request.monto().stripTrailingZeros().toPlainString(),
                request.concepto().trim().toLowerCase(Locale.ROOT)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
