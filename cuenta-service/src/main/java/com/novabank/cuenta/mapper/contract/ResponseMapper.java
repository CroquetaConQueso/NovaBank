package com.novabank.cuenta.mapper.contract;

public interface ResponseMapper<E, R> {
    R toResponse(E entity);
}
