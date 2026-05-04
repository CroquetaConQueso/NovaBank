package com.novabank.cuenta.client;

import com.novabank.cuenta.dto.ClienteResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cliente-service", configuration = ClienteServiceFeignConfig.class)
public interface ClienteServiceClient {

    @GetMapping("/api/clientes/{id}")
    ClienteResponseDTO obtenerCliente(@PathVariable Long id);
}
