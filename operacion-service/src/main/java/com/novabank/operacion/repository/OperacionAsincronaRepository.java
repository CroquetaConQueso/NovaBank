package com.novabank.operacion.repository;

import com.novabank.operacion.model.OperacionAsincrona;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface OperacionAsincronaRepository extends ReactiveCrudRepository<OperacionAsincrona, UUID> {
}
