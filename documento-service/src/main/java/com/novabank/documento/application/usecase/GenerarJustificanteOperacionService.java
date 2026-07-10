package com.novabank.documento.application.usecase;

import com.novabank.documento.application.port.in.GenerarJustificanteOperacionCommand;
import com.novabank.documento.application.port.in.GenerarJustificanteOperacionUseCase;
import com.novabank.documento.application.port.out.DocumentoStoragePort;
import com.novabank.documento.application.port.out.GeneratedJustificante;
import com.novabank.documento.application.port.out.JustificanteGeneratorPort;
import com.novabank.documento.domain.model.DocumentoId;
import com.novabank.documento.domain.model.DocumentoOperacion;
import com.novabank.documento.domain.model.TipoDocumento;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class GenerarJustificanteOperacionService implements GenerarJustificanteOperacionUseCase {

    public static final long CUENTA_NO_DETERMINADA = 0L;

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM").withZone(ZoneOffset.UTC);

    private final JustificanteGeneratorPort generatorPort;
    private final DocumentoStoragePort storagePort;

    public GenerarJustificanteOperacionService(
            JustificanteGeneratorPort generatorPort,
            DocumentoStoragePort storagePort
    ) {
        this.generatorPort = generatorPort;
        this.storagePort = storagePort;
    }

    @Override
    public Mono<DocumentoOperacion> generar(GenerarJustificanteOperacionCommand command) {
        return validar(command)
                .then(generatorPort.generar(command))
                .flatMap(justificante -> storagePort.guardar(toDocumento(command, justificante), justificante.contenido()));
    }

    private Mono<Void> validar(GenerarJustificanteOperacionCommand command) {
        if (command == null || command.operationId() == null) {
            return Mono.error(new IllegalArgumentException("operationId es obligatorio para generar justificante"));
        }
        if (command.tipoOperacion() == null || command.tipoOperacion().isBlank()) {
            return Mono.error(new IllegalArgumentException("tipoOperacion es obligatorio para generar justificante"));
        }
        return Mono.empty();
    }

    private DocumentoOperacion toDocumento(
            GenerarJustificanteOperacionCommand command,
            GeneratedJustificante justificante
    ) {
        Instant fechaOperacion = command.occurredAt() == null ? Instant.now() : command.occurredAt();
        Long cuentaId = resolverCuenta(command);
        String claveObjeto = "cuentas/%d/operaciones/%s/%s/%s.json".formatted(
                cuentaId,
                YEAR.format(fechaOperacion),
                MONTH.format(fechaOperacion),
                command.operationId()
        );

        return new DocumentoOperacion(
                DocumentoId.nuevo(),
                command.operationId(),
                cuentaId,
                claveObjeto,
                TipoDocumento.JUSTIFICANTE_OPERACION,
                justificante.contentType(),
                Instant.now()
        );
    }

    private Long resolverCuenta(GenerarJustificanteOperacionCommand command) {
        if (command.cuentaId() != null) {
            return command.cuentaId();
        }
        if ("DEPOSITO".equalsIgnoreCase(command.tipoOperacion()) && command.cuentaDestinoId() != null) {
            return command.cuentaDestinoId();
        }
        if (command.cuentaOrigenId() != null) {
            return command.cuentaOrigenId();
        }
        return CUENTA_NO_DETERMINADA;
    }
}
