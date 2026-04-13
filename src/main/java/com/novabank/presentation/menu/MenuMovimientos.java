package com.novabank.presentation.menu;

import com.novabank.domain.model.Cuenta;
import com.novabank.exception.NovaBankException;
import com.novabank.service.MovimientoServicio;

import java.math.BigDecimal;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Menú de operaciones financieras.
 *
 * Se encarga de la interacción por consola y delega la lógica
 * de negocio en MovimientoServicio.
 */
public class MenuMovimientos {

    private final MovimientoServicio moviServ;
    private final Scanner entrada;

    public MenuMovimientos(MovimientoServicio moviServ, Scanner entrada) {
        this.moviServ = moviServ;
        this.entrada = entrada;
    }

    public void depositarDinero() {
        try {
            System.out.print("Número de cuenta: ");
            String numeroCuenta = entrada.nextLine().trim().toUpperCase();

            System.out.print("Cantidad a depositar: ");
            BigDecimal cantidad = entrada.nextBigDecimal();
            entrada.nextLine();

            Cuenta cuenta = moviServ.depositar(numeroCuenta, cantidad);

            System.out.println("Depósito realizado correctamente.");
            System.out.println("Cuenta: " + cuenta.getNumeroCuenta()
                    + "\nImporte: +" + cantidad + " €"
                    + "\nNuevo saldo: " + cuenta.getSaldoCuenta() + " €");

        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            System.err.println("El valor debe de ser numérico");
            entrada.nextLine();
        }
    }

    public void retirarDinero() {
        try {
            System.out.print("Número de cuenta: ");
            String numeroCuenta = entrada.nextLine().trim().toUpperCase();

            System.out.print("Cantidad a retirar: ");
            BigDecimal cantidad = entrada.nextBigDecimal();
            entrada.nextLine();

            Cuenta cuenta = moviServ.retirar(numeroCuenta, cantidad);

            System.out.println("Retiro realizado correctamente.");
            System.out.println("Cuenta: " + cuenta.getNumeroCuenta()
                    + "\nImporte: -" + cantidad + " €"
                    + "\nNuevo saldo: " + cuenta.getSaldoCuenta() + " €");

        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            System.err.println("El valor debe de ser numérico");
            entrada.nextLine();
        }
    }

    public void transferirDinero() {
        try {
            System.out.print("Número de cuenta origen: ");
            String numeroOrigen = entrada.nextLine().trim().toUpperCase();

            System.out.print("Número de cuenta destino: ");
            String numeroDestino = entrada.nextLine().trim().toUpperCase();

            System.out.print("Cantidad a transferir: ");
            BigDecimal cantidad = entrada.nextBigDecimal();
            entrada.nextLine();

            moviServ.transferir(numeroOrigen, numeroDestino, cantidad);

            System.out.println("Transferencia realizada correctamente.");
            System.out.println("Cuenta origen: " + numeroOrigen + " -> -" + cantidad + " €");
            System.out.println("Cuenta destino: " + numeroDestino + " -> +" + cantidad + " €");

        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            System.err.println("El valor debe de ser numérico");
            entrada.nextLine();
        }
    }

    public void menuMovimientos() {
        while (true) {
            System.out.println();
            System.out.println("--- OPERACIONES FINANCIERAS ---");
            System.out.println("1. Depositar dinero");
            System.out.println("2. Retirar dinero");
            System.out.println("3. Transferencia entre cuentas");
            System.out.println("4. Volver");
            System.out.print("Seleccione una opción: ");

            try {
                int opcionSwitch = entrada.nextInt();
                entrada.nextLine();

                switch (opcionSwitch) {
                    case 1 -> depositarDinero();
                    case 2 -> retirarDinero();
                    case 3 -> transferirDinero();
                    case 4 -> {
                        System.out.println("Volviendo al menú principal...");
                        return;
                    }
                    default -> System.err.println("Debes de escoger una opción encontrada en el menú");
                }
            } catch (InputMismatchException ex) {
                System.err.println("Debes introducir un valor numérico");
                entrada.nextLine();
            }
        }
    }
}