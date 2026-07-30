package com.novabank.operacion.adapter.in.web;

import com.novabank.operacion.application.port.in.ConsultarEstadoOperacionQuery;
import com.novabank.operacion.application.port.in.ConsultarEstadoOperacionUseCase;
import com.novabank.operacion.application.port.in.EstadoOperacionAsincronaResult;
import com.novabank.operacion.application.port.in.OperacionAceptadaResult;
import com.novabank.operacion.application.port.in.SolicitarDepositoCommand;
import com.novabank.operacion.application.port.in.SolicitarDepositoUseCase;
import com.novabank.operacion.application.port.in.SolicitarRetiradaCommand;
import com.novabank.operacion.application.port.in.SolicitarRetiradaUseCase;
import com.novabank.operacion.application.port.in.SolicitarTransferenciaCommand;
import com.novabank.operacion.application.port.in.SolicitarTransferenciaUseCase;
import com.novabank.operacion.application.port.in.TransferenciaAceptadaResult;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionAceptadaResponseDTO;
import com.novabank.operacion.dto.OperacionEstadoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaAceptadaResponseDTO;
import com.novabank.operacion.dto.TransferenciaDivisaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.service.OperacionService;
import com.novabank.operacion.tracing.CorrelationIdSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/operaciones")
@Tag(name = "Operaciones", description = "Operaciones financieras y consulta de movimientos")
public class OperacionController {

    private final SolicitarDepositoUseCase solicitarDepositoUseCase;
    private final SolicitarRetiradaUseCase solicitarRetiradaUseCase;
    private final SolicitarTransferenciaUseCase solicitarTransferenciaUseCase;
    private final ConsultarEstadoOperacionUseCase consultarEstadoOperacionUseCase;
    private final OperacionService operacionService;

    public OperacionController(
            SolicitarDepositoUseCase solicitarDepositoUseCase,
            SolicitarRetiradaUseCase solicitarRetiradaUseCase,
            SolicitarTransferenciaUseCase solicitarTransferenciaUseCase,
            ConsultarEstadoOperacionUseCase consultarEstadoOperacionUseCase,
            OperacionService operacionService
    ) {
        this.solicitarDepositoUseCase = solicitarDepositoUseCase;
        this.solicitarRetiradaUseCase = solicitarRetiradaUseCase;
        this.solicitarTransferenciaUseCase = solicitarTransferenciaUseCase;
        this.consultarEstadoOperacionUseCase = consultarEstadoOperacionUseCase;
        this.operacionService = operacionService;
    }

    @PostMapping("/deposito")
    @Operation(
            summary = "Realizar deposito",
            description = "Publica una solicitud asincrona para que cuenta-service aplique el deposito."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Deposito solicitado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "503", description = "Kafka no disponible o evento no publicado")
    })
    public Mono<ResponseEntity<OperacionAceptadaResponseDTO>> depositar(
            @Valid @RequestBody OperacionRequestDTO request,
            @Parameter(description = "Clave opcional para idempotencia publica de la operacion")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return Mono.deferContextual(context -> solicitarDepositoUseCase.solicitarDeposito(new SolicitarDepositoCommand(
                        request.cuentaId(),
                        request.cantidad(),
                        idempotencyKey,
                        resolveCorrelationId(CorrelationIdSupport.fromContext(context))
                )))
                .map(this::toOperacionAceptadaResponse)
                .map(response -> ResponseEntity.accepted().body(response));
    }

    @PostMapping("/retiro")
    @Operation(
            summary = "Realizar retiro",
            description = "Publica una solicitud asincrona para que cuenta-service aplique el retiro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Retiro solicitado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "503", description = "Kafka no disponible o evento no publicado")
    })
    public Mono<ResponseEntity<OperacionAceptadaResponseDTO>> retirar(
            @Valid @RequestBody OperacionRequestDTO request,
            @Parameter(description = "Clave opcional para idempotencia publica de la operacion")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return Mono.deferContextual(context -> solicitarRetiradaUseCase.solicitarRetirada(new SolicitarRetiradaCommand(
                        request.cuentaId(),
                        request.cantidad(),
                        idempotencyKey,
                        resolveCorrelationId(CorrelationIdSupport.fromContext(context))
                )))
                .map(this::toOperacionAceptadaResponse)
                .map(response -> ResponseEntity.accepted().body(response));
    }

    @PostMapping("/transferencia")
    @Operation(
            summary = "Realizar transferencia",
            description = "Publica una solicitud asincrona para que cuenta-service aplique la transferencia ordinaria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transferencia solicitada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "503", description = "Kafka no disponible o evento no publicado")
    })
    public Mono<ResponseEntity<TransferenciaAceptadaResponseDTO>> transferir(
            @Valid @RequestBody TransferenciaRequestDTO request,
            @Parameter(description = "Clave opcional para idempotencia publica de la transferencia")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return Mono.deferContextual(context -> solicitarTransferenciaUseCase.solicitarTransferencia(
                        new SolicitarTransferenciaCommand(
                                request.cuentaOrigenId(),
                                request.cuentaDestinoId(),
                                request.cantidad(),
                                idempotencyKey,
                                resolveCorrelationId(CorrelationIdSupport.fromContext(context)),
                                request.internacional(),
                                request.paisDestino(),
                                request.tipoCliente()
                        )
                ))
                .map(this::toTransferenciaAceptadaResponse)
                .map(response -> ResponseEntity.accepted().body(response));
    }

    @PostMapping("/transferencias/divisa")
    @Operation(
            summary = "Realizar transferencia en divisa",
            description = "Consulta una tasa de cambio fiable antes de solicitar a cuenta-service la actualizacion de saldos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferencia en divisa realizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Cuenta origen o destino no encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente"),
            @ApiResponse(responseCode = "503", description = "Tipo de cambio no disponible o servicio remoto no disponible")
    })
    public Mono<OperacionResponseDTO> transferirEnDivisa(
            @Valid @RequestBody TransferenciaDivisaRequestDTO request,
            @Parameter(description = "Clave opcional para idempotencia publica de la transferencia en divisa")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return operacionService.transferirEnDivisa(request, idempotencyKey);
    }

    @GetMapping("/cuentas/{cuentaId}/movimientos")
    @Operation(
            summary = "Listar movimientos de una cuenta",
            description = "Devuelve el historial registrado por operacion-service, con filtro opcional por fechas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos obtenidos correctamente"),
            @ApiResponse(responseCode = "400", description = "Identificador o rango de fechas invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway")
    })
    public Flux<MovimientoResponseDTO> listarMovimientos(
            @PathVariable Long cuentaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        return operacionService.listarMovimientos(cuentaId, fechaInicio, fechaFin);
    }

    @GetMapping("/sagas/{operationId}")
    @Operation(
            summary = "Consultar estado de operacion asincrona",
            description = "Devuelve el estado persistido de una operacion asincrona del Modulo 6."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado obtenido correctamente"),
            @ApiResponse(responseCode = "400", description = "OperationId invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido al acceder mediante Gateway"),
            @ApiResponse(responseCode = "404", description = "Operacion asincrona no encontrada")
    })
    public Mono<OperacionEstadoResponseDTO> consultarSaga(@PathVariable UUID operationId) {
        return consultarEstadoOperacionUseCase.consultar(new ConsultarEstadoOperacionQuery(operationId))
                .map(this::toOperacionEstadoResponse);
    }

    private OperacionAceptadaResponseDTO toOperacionAceptadaResponse(OperacionAceptadaResult result) {
        return new OperacionAceptadaResponseDTO(
                result.operationId(),
                result.estado(),
                result.mensaje(),
                result.tipoOperacion(),
                result.cuentaId(),
                result.importe()
        );
    }

    private TransferenciaAceptadaResponseDTO toTransferenciaAceptadaResponse(TransferenciaAceptadaResult result) {
        return new TransferenciaAceptadaResponseDTO(
                result.operationId(),
                result.estado(),
                result.mensaje(),
                result.tipoOperacion(),
                result.cuentaOrigenId(),
                result.cuentaDestinoId(),
                result.importe(),
                result.moneda(),
                result.comision(),
                result.tasaComision()
        );
    }

    private OperacionEstadoResponseDTO toOperacionEstadoResponse(EstadoOperacionAsincronaResult result) {
        return new OperacionEstadoResponseDTO(
                result.operationId(),
                result.correlationId(),
                result.tipoOperacion(),
                result.estado(),
                result.cuentaId(),
                result.cuentaOrigenId(),
                result.cuentaDestinoId(),
                result.importe(),
                result.moneda(),
                result.motivoFallo(),
                result.creadaEn(),
                result.actualizadaEn()
        );
    }

    private UUID resolveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID();
        }

        try {
            return UUID.fromString(correlationId);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }
}
