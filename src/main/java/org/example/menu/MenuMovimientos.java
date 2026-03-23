package org.example.menu;

import lombok.AllArgsConstructor;
import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.servicios.MovimientoServicio;

import java.math.BigDecimal;
import java.util.Scanner;

@AllArgsConstructor
public class MenuMovimientos {
    private final MovimientoServicio movimientoServicio;
    private Scanner entrada;

    private void transferirDinero(){
        try {
            System.out.print("Número de cuenta origen: ");
            String numeroCuOrigen = entrada.nextLine().toUpperCase().trim();

            System.out.print("Número de cuenta destino: ");
            String numeroCuDestino = entrada.nextLine().trim().toUpperCase();

            System.out.print("Cantidad a transferir: ");
            BigDecimal cantidad = entrada.nextBigDecimal();

            movimientoServicio.transferir(numeroCuOrigen, numeroCuDestino, cantidad);

            System.out.println("Transferencia realizada correctamente.");
            System.out.println("Cuenta origen: " + numeroCuOrigen + " -> -" + cantidad);
            System.out.println("Cuenta destino: " + numeroCuDestino + " -> +" + cantidad);

        } catch (IllegalArgumentException ex) {
            System.out.println("ERROR: " + ex.getMessage());
        }
    }

    private void retirarDinero(){
        try {
            System.out.print("Número de cuenta: ");
            String numeroCuenta = entrada.nextLine().toUpperCase().trim();

            System.out.print("Cantidad a retirar: ");
            BigDecimal cantidad = entrada.nextBigDecimal();

            Cuenta cuenta = movimientoServicio.retirar(numeroCuenta, cantidad);

            System.out.println("Retiro realizado correctamente.");
            System.out.println("Depósito realizado correctamente.\nCuenta: " + cuenta.getNumeroCuenta()
                    +"\nImporte: +" + cantidad+"\nNuevo saldo: " + cuenta.getSaldoCuenta());
        } catch (IllegalArgumentException ex) {
            System.out.println("ERROR: " + ex.getMessage());
        }
    }

    private void depositarDinero(){
        try {
            System.out.print("Número de cuenta: ");
            String numeroCuento = entrada.nextLine().trim().toUpperCase();

            System.out.print("Cantidad a depositar: ");
            BigDecimal cantidad = entrada.nextBigDecimal();

            Cuenta cuenta = movimientoServicio.depositar(numeroCuento, cantidad);

            System.out.println("Depósito realizado correctamente.\nCuenta: " + cuenta.getNumeroCuenta()
            +"\nImporte: +" + cantidad+"\nNuevo saldo: " + cuenta.getSaldoCuenta());

        } catch (IllegalArgumentException ex) {
            System.out.println("ERROR: " + ex.getMessage());
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
                        System.out.println("Debes escoger una opción encontrada en el menu");
                }

            } catch (NumberFormatException ex) {
                System.out.println("ERROR: Debes introducir un valor numérico.");
            }
        }
    }
}
