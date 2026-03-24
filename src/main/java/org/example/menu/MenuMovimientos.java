package org.example.menu;

import org.example.modelos.Cuenta;
import org.example.servicios.MovimientoServicio;

import java.math.BigDecimal;
import java.util.InputMismatchException;
import java.util.Scanner;

public class MenuMovimientos {

    private final MovimientoServicio movimientoServicio;
    private Scanner entrada;

    public MenuMovimientos(MovimientoServicio movimientoServicio, Scanner entrada) {
        this.movimientoServicio = movimientoServicio;
        this.entrada = entrada;
    }

    private void transferirDinero(){
        try {
            System.out.print("Número de cuenta origen: ");
            String numeroCuOrigen = entrada.nextLine().toUpperCase().trim();

            System.out.print("Número de cuenta destino: ");
            String numeroCuDestino = entrada.nextLine().trim().toUpperCase();

            System.out.print("Cantidad a transferir: ");
            BigDecimal cantidad = entrada.nextBigDecimal();
            entrada.nextLine();

            movimientoServicio.transferir(numeroCuOrigen, numeroCuDestino, cantidad);

            System.out.println("Transferencia realizada correctamente."
                    +"\nCuenta origen: " + numeroCuOrigen + " -> -" + cantidad+" €"
                    +"\nCuenta destino: " + numeroCuDestino + " -> +" + cantidad+" €");

        } catch (InputMismatchException inex) {
            System.err.println("ERROR: La cantidad debe de tener un valor númerico válido");
            entrada.nextLine();
        }  catch(RuntimeException ruex){
            System.err.println("ERROR: " + ruex.getMessage());
        }
    }

    private void retirarDinero(){
        try {
            System.out.print("Número de cuenta: ");
            String numeroCuenta = entrada.nextLine().toUpperCase().trim();

            System.out.print("Cantidad a retirar: ");
            BigDecimal cantidad = entrada.nextBigDecimal();
            entrada.nextLine();

            Cuenta cuenta = movimientoServicio.retirar(numeroCuenta, cantidad);

            System.out.println("Retiro realizado correctamente.\nCuenta: " + cuenta.getNumeroCuenta()
                    +"\nImporte: -" + cantidad+" €"+"\nNuevo saldo: " + cuenta.getSaldoCuenta()+" €");
        } catch (InputMismatchException inex) {
            System.err.println("ERROR: La cantidad debe de tener un valor numérico");
            entrada.nextLine();
        } catch (IllegalArgumentException ex){
            System.err.println("ERROR: "+ex.getMessage());
        }
    }

    private void depositarDinero(){
        try {
            System.out.print("Número de cuenta: ");
            String numeroCuento = entrada.nextLine().trim().toUpperCase();

            System.out.print("Cantidad a depositar: ");
            BigDecimal cantidad = entrada.nextBigDecimal();
            entrada.nextLine();

            Cuenta cuenta = movimientoServicio.depositar(numeroCuento, cantidad);

            System.out.println("Depósito realizado correctamente.\nCuenta: " + cuenta.getNumeroCuenta()
                    +"\nImporte: +" + cantidad+" €"+"\nNuevo saldo: " + cuenta.getSaldoCuenta()+" €");

        } catch (InputMismatchException inex) {
            System.err.println("ERROR: La cantidad debe de tener un valor númerico");
            entrada.nextLine();
        }catch (IllegalArgumentException ex){
            System.err.println("ERROR: " + ex.getMessage());
        }
    }

    public void menuMovimientos(){
        while (true) {
            System.out.println();
            System.out.println("--- OPERACIONES FINANCIERAS --");
            System.out.println("1. Depositar dinero");
            System.out.println("2. Retirar dinero");
            System.out.println("3. Transferencia entre cuentas");
            System.out.println("4. Volver");
            System.out.print("Seleccione una opción: ");

            try {
                int opcionSwitch = entrada.nextInt();
                entrada.nextLine();

                switch (opcionSwitch) {
                    case 1:
                        depositarDinero();
                        break;
                    case 2:
                        retirarDinero();
                        break;
                    case 3:
                        transferirDinero();
                        break;
                    case 4:
                        System.out.println("Volviendo al menú principal...");
                        return;
                    default:
                        System.err.println("Debes escoger una opción encontrada en el menu");
                }

            } catch (InputMismatchException inex) {
                System.err.println("ERROR: Debes introducir un valor numérico.");
                entrada.nextLine();
            } catch (IllegalArgumentException ex){
                System.err.println("ERROR: Debes de introducir un valor numérico");
            }
        }
    }
}