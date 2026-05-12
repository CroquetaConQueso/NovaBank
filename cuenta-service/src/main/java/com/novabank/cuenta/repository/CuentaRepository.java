package com.novabank.cuenta.repository;

import com.novabank.cuenta.model.Cuenta;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CuentaRepository extends ReactiveCrudRepository<Cuenta, Long> {

    Mono<Cuenta> findByNumeroCuenta(String numeroCuenta);

    Mono<Boolean> existsByNumeroCuenta(String numeroCuenta);

    Flux<Cuenta> findByClienteId(Long clienteId);
}
