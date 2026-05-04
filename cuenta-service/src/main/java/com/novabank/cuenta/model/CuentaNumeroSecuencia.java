package com.novabank.cuenta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_number_sequence")
public class CuentaNumeroSecuencia {

    @Id
    private Long id;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;

    protected CuentaNumeroSecuencia() {
    }

    public CuentaNumeroSecuencia(Long id, Long nextValue) {
        this.id = id;
        this.nextValue = nextValue;
    }

    public Long getId() {
        return id;
    }

    public Long getNextValue() {
        return nextValue;
    }

    public void setNextValue(Long nextValue) {
        this.nextValue = nextValue;
    }
}
