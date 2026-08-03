package com.novabank.lambda.comision;

import java.math.BigDecimal;

public class ComisionResponse {

    private BigDecimal comision;
    private BigDecimal tasaAplicada;
    private String paisDestino;
    private String tipoCliente;

    public ComisionResponse() {
    }

    public ComisionResponse(
            BigDecimal comision,
            BigDecimal tasaAplicada,
            String paisDestino,
            String tipoCliente
    ) {
        this.comision = comision;
        this.tasaAplicada = tasaAplicada;
        this.paisDestino = paisDestino;
        this.tipoCliente = tipoCliente;
    }

    public BigDecimal getComision() {
        return comision;
    }

    public void setComision(BigDecimal comision) {
        this.comision = comision;
    }

    public BigDecimal getTasaAplicada() {
        return tasaAplicada;
    }

    public void setTasaAplicada(BigDecimal tasaAplicada) {
        this.tasaAplicada = tasaAplicada;
    }

    public String getPaisDestino() {
        return paisDestino;
    }

    public void setPaisDestino(String paisDestino) {
        this.paisDestino = paisDestino;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }
}
