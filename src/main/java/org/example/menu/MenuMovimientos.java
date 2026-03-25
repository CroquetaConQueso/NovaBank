package org.example.menu;

import org.example.modelos.Cuenta;
import org.example.servicios.MovimientoServicio;
import org.example.utilidades.Utilidades;

import java.math.BigDecimal;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Gestiona las operaciones financieras realizadas sobre las cuentas.
 *
 * Permite efectuar depósitos, retiros y transferencias entre cuentas,
 * delegando la lógica de negocio en MovimientoServicio.
 *
 * Actúa como capa de presentación dentro del sistema.
 */
public class MenuMovimientos {

    private final MovimientoServicio movimientoServicio;
    private Scanner entrada;

    public MenuMovimientos(MovimientoServicio movimientoServicio, Scanner entrada) {
        this.movimientoServicio = movimientoServicio;
        this.entrada = entrada;
    }

    private String tomarNumeroCuenta(String mensaje){
        System.out.print(mensaje);
        String numeroCuenta = entrada.nextLine().trim().toUpperCase();

        if (!Utilidades.validarNumeroCuenta(numeroCuenta)) {
            throw new IllegalArgumentException(
                    "El número de cuenta debe tener formato ES seguido de 20 dígitos"
            );
        }
        return numeroCuenta;
    }

    /**
     * Solicita los datos necesarios para realizar una transferencia
     * entre dos cuentas y muestra el resultado por output.
     *
     * La validación de cuentas y saldo se delega en el servicio.
     */
    private void transferirDinero(){
        try {
            String numeroCuOrigen = tomarNumeroCuenta("Número de cuenta origen: ");

            String numeroCuDestino = tomarNumeroCuenta("Número de cuenta destino: ");

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

    /**
     * Permite retirar una cantidad de dinero de una cuenta específica,
     * mostrando el nuevo saldo tras la operación.
     */
    private void retirarDinero(){
        try {
            String numeroCuenta = tomarNumeroCuenta("Número de cuenta: ");

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

    /**
     * Permite ingresar una cantidad de dinero en una cuenta,
     * actualizando y mostrando el saldo resultante.
     */
    private void depositarDinero(){
        try {
            String numeroCuento = tomarNumeroCuenta("Número de cuenta: ");

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

    /**
     * Muestra el menú interactivo de operaciones financieras y
     * gestiona la navegación entre las distintas opciones.
     *
     * Permanece activo hasta que el usuario decide volver
     * al menú principal.
     */
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