package com.novabank.lambda.comision;

import java.math.BigDecimal;

public class ComisionRequest {

    private BigDecimal importeEuros;
    private String paisDestino;
    private String tipoCliente;

    public ComisionRequest() {
    }

    public ComisionRequest(BigDecimal importeEuros, String paisDestino, String tipoCliente) {
        this.importeEuros = importeEuros;
        this.paisDestino = paisDestino;
        this.tipoCliente = tipoCliente;
    }

    public BigDecimal getImporteEuros() {
        return importeEuros;
    }

    public void setImporteEuros(BigDecimal importeEuros) {
        this.importeEuros = importeEuros;
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
