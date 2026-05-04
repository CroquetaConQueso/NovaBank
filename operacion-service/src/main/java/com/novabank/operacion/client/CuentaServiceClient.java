package com.novabank.operacion.client;

import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "cuenta-service",
        configuration = CuentaServiceFeignConfig.class,
        fallbackFactory = CuentaServiceClientFallbackFactory.class
)
public interface CuentaServiceClient {

    @PostMapping("/internal/cuentas/{id}/depositos")
    MovimientoResponseDTO depositar(@PathVariable Long id, @RequestBody CuentaOperacionRequestDTO request);

    @PostMapping("/internal/cuentas/{id}/retiros")
    MovimientoResponseDTO retirar(@PathVariable Long id, @RequestBody CuentaOperacionRequestDTO request);

    @PostMapping("/internal/cuentas/transferencias")
    List<MovimientoResponseDTO> transferir(@RequestBody TransferenciaInternaRequestDTO request);
}
