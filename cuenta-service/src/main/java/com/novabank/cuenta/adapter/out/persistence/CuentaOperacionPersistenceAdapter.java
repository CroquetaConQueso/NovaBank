package com.novabank.cuenta.adapter.out.persistence;

import com.novabank.cuenta.application.port.out.AplicarOperacionCuentaPort;
import com.novabank.cuenta.dto.CuentaOperacionRequestDTO;
import com.novabank.cuenta.dto.TransferenciaInternaRequestDTO;
import com.novabank.cuenta.service.CuentaService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class CuentaOperacionPersistenceAdapter implements AplicarOperacionCuentaPort {

    private final CuentaService cuentaService;

    public CuentaOperacionPersistenceAdapter(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @Override
    public Mono<Void> depositar(Long cuentaId, BigDecimal importe) {
        return cuentaService.depositar(cuentaId, new CuentaOperacionRequestDTO(importe))
                .then();
    }

    @Override
    public Mono<Void> retirar(Long cuentaId, BigDecimal importe) {
        return cuentaService.retirar(cuentaId, new CuentaOperacionRequestDTO(importe))
                .then();
    }

    @Override
    public Mono<Void> transferir(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal importe) {
        return cuentaService.transferir(new TransferenciaInternaRequestDTO(cuentaOrigenId, cuentaDestinoId, importe))
                .then();
    }
}
