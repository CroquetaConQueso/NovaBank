package com.novabank.util;

/**
 * Clase de utilidades compartidas del proyecto.
 *
 * Debe contener únicamente lógica reutilizable, estática y sin estado.
 */
public final class Utilidades {

    /**
     * Impide la instanciación de la clase de utilidades.
     */
    private Utilidades() {
    }

    /**
     * Valida que el número de cuenta siga el formato simplificado del proyecto:
     * ES seguido de 20 dígitos.
     *
     * @param numeroCuenta valor a validar
     * @return true si el formato es correcto; false en caso contrario
     */
    public static boolean validarNumeroCuenta(String numeroCuenta) {
        return numeroCuenta != null && numeroCuenta.matches("ES\\d{20}");
    }
}