package com.novabank.lambda.comision;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComisionHandlerTest {

    private final ComisionHandler handler = new ComisionHandler();

    @Test
    void calculaComisionParaParticularConPaisConocido() {
        ComisionResponse response = handler.handleRequest(
                new ComisionRequest(new BigDecimal("1000"), "US", "PARTICULAR"),
                null
        );

        assertEquals(new BigDecimal("20.00"), response.getComision());
        assertEquals(new BigDecimal("0.020"), response.getTasaAplicada());
        assertEquals("US", response.getPaisDestino());
        assertEquals("PARTICULAR", response.getTipoCliente());
    }

    @Test
    void aplicaDescuentoParaClienteEmpresa() {
        ComisionResponse response = handler.handleRequest(
                new ComisionRequest(new BigDecimal("1000"), "US", "EMPRESA"),
                null
        );

        assertEquals(new BigDecimal("16.00"), response.getComision());
        assertEquals(new BigDecimal("0.016"), response.getTasaAplicada());
        assertEquals("EMPRESA", response.getTipoCliente());
    }

    @Test
    void usaTasaDefaultParaPaisDesconocido() {
        ComisionResponse response = handler.handleRequest(
                new ComisionRequest(new BigDecimal("1000"), "JP", "PARTICULAR"),
                null
        );

        assertEquals(new BigDecimal("25.00"), response.getComision());
        assertEquals(new BigDecimal("0.025"), response.getTasaAplicada());
        assertEquals("JP", response.getPaisDestino());
    }

    @Test
    void tipoClienteDesconocidoSeTrataComoParticular() {
        ComisionResponse response = handler.handleRequest(
                new ComisionRequest(new BigDecimal("1000"), "GB", "VIP"),
                null
        );

        assertEquals(new BigDecimal("18.00"), response.getComision());
        assertEquals(new BigDecimal("0.018"), response.getTasaAplicada());
        assertEquals("PARTICULAR", response.getTipoCliente());
    }

    @Test
    void importeInvalidoLanzaExcepcionControlada() {
        ComisionRequest request = new ComisionRequest(BigDecimal.ZERO, "US", "PARTICULAR");

        assertThrows(ComisionValidationException.class, () -> handler.handleRequest(request, null));
    }

    @Test
    void redondeaComisionAHalfUpConDosDecimales() {
        ComisionResponse response = handler.handleRequest(
                new ComisionRequest(new BigDecimal("333.335"), "MX", "PARTICULAR"),
                null
        );

        assertEquals(new BigDecimal("10.00"), response.getComision());
        assertEquals(new BigDecimal("0.030"), response.getTasaAplicada());
    }

    @Test
    void normalizaEntrada() {
        ComisionResponse response = handler.handleRequest(
                new ComisionRequest(new BigDecimal("1000"), " us ", " empresa "),
                null
        );

        assertEquals(new BigDecimal("16.00"), response.getComision());
        assertEquals("US", response.getPaisDestino());
        assertEquals("EMPRESA", response.getTipoCliente());
    }
}
