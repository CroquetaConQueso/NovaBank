package com.novabank.operacion.service;

import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperacionService {

    private final CuentaServiceClient cuentaServiceClient;

    public OperacionService(CuentaServiceClient cuentaServiceClient) {
        this.cuentaServiceClient = cuentaServiceClient;
    }

    public OperacionResponseDTO depositar(OperacionRequestDTO request) {
        MovimientoResponseDTO movimiento = cuentaServiceClient.depositar(
                request.cuentaId(),
                new CuentaOperacionRequestDTO(request.cantidad())
        );

        return new OperacionResponseDTO(
                "DEPOSITO",
                "Deposito realizado correctamente",
                List.of(movimiento)
        );
    }

    public OperacionResponseDTO retirar(OperacionRequestDTO request) {
        MovimientoResponseDTO movimiento = cuentaServiceClient.retirar(
                request.cuentaId(),
                new CuentaOperacionRequestDTO(request.cantidad())
        );

        return new OperacionResponseDTO(
                "RETIRO",
                "Retiro realizado correctamente",
                List.of(movimiento)
        );
    }

    public OperacionResponseDTO transferir(TransferenciaRequestDTO request) {
        List<MovimientoResponseDTO> movimientos = cuentaServiceClient.transferir(new TransferenciaInternaRequestDTO(
                request.cuentaOrigenId(),
                request.cuentaDestinoId(),
                request.cantidad()
        ));

        return new OperacionResponseDTO(
                "TRANSFERENCIA",
                "Transferencia realizada correctamente",
                movimientos
        );
    }
}
