package com.novabank.mapper.contract;

public interface RequestMapper<D, E> {
    E toEntity(D dto);
}
