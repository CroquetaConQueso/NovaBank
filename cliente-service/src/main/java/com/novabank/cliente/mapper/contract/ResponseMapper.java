package com.novabank.cliente.mapper.contract;

public interface ResponseMapper<E, R> {
    R toResponse(E entity);
}
