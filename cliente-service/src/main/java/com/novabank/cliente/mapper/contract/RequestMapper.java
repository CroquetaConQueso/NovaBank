package com.novabank.cliente.mapper.contract;

public interface RequestMapper<D, E> {
    E toEntity(D dto);
}
