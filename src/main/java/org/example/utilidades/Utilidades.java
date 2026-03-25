package org.example.utilidades;

import java.util.Scanner;

public class Utilidades {
    private static Scanner entrada;

    public Utilidades(Scanner entrada){
        this.entrada = entrada;
    }

    public static boolean validarNumeroCuenta(String numeroCuenta){
        return numeroCuenta != null && numeroCuenta.matches("ES\\d{20}");
    }
}
