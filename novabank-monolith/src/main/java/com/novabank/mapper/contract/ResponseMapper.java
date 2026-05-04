package com.novabank.mapper.contract;

public interface ResponseMapper<E, R> {
    R toResponse(E entity);
}
