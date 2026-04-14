package com.novabank.presentation.menu;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.NovaBankException;
import com.novabank.service.CuentaServicio;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Menú de cuentas.
 *
 * Se limita a recoger entradas, mostrar resultados y delegar las reglas
 * funcionales en CuentaServicio.
 */
public class MenuCuenta {

    private final CuentaServicio cuentaServicio;
    private final Scanner entrada;

    public MenuCuenta(CuentaServicio cuentaServicio, Scanner entrada) {
        this.cuentaServicio = cuentaServicio;
        this.entrada = entrada;
    }

    public void menuCuentas() {
        while (true) {
            System.out.println();
            System.out.println("--- GESTIÓN DE CUENTAS ---");
            System.out.println("1. Crear cuenta");
            System.out.println("2. Listar cuentas de cliente");
            System.out.println("3. Ver información de cuenta");
            System.out.println("4. Volver");
            System.out.print("Seleccione una opción: ");

            try {
                int opcionSwitch = entrada.nextInt();
                entrada.nextLine();

                switch (opcionSwitch) {
                    case 1 -> crearCuenta();
                    case 2 -> listarCuentasCli();
                    case 3 -> verInfoCuenta();
                    case 4 -> {
                        System.out.println("Volviendo al menú principal...");
                        return;
                    }
                    default -> System.err.println("ERROR: Debes escoger una opción válida del menú.");
                }
            } catch (InputMismatchException ex) {
                System.err.println("ERROR: Debes introducir un valor numérico.");
                entrada.nextLine();
            }
        }
    }

    public void crearCuenta() {
        try {
            System.out.print("ID del cliente titular de la cuenta: ");
            Long idCliente = entrada.nextLong();
            entrada.nextLine();

            Cuenta cuenta = cuentaServicio.crearCuenta(idCliente);

            System.out.println("Cuenta creada correctamente.");
            System.out.println("Número de cuenta: " + cuenta.getNumeroCuenta());
            System.out.println(
                    "Titular: " + cuenta.getDueñoCuenta().getNombreCliente()
                            + " " + cuenta.getDueñoCuenta().getApellidosCliente()
                            + " (ID: " + cuenta.getDueñoCuenta().getIdCliente() + ")"
            );
            System.out.println("Saldo inicial: " + formatearImporte(cuenta.getSaldoCuenta()));
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            System.err.println("ERROR: El valor debe ser numérico.");
            entrada.nextLine();
        }
    }

    public void listarCuentasCli() {
        try {
            System.out.print("Introduzca ID del cliente: ");
            Long idCliente = entrada.nextLong();
            entrada.nextLine();

            Cliente cliente = cuentaServicio.obtenerTitular(idCliente);
            List<Cuenta> cuentasCliente = cuentaServicio.obtenerCuentas(idCliente);

            System.out.println("Cuentas del cliente " + cliente.getNombreCliente() + " " + cliente.getApellidosCliente() + ":");

            if (cuentasCliente.isEmpty()) {
                System.out.println("No hay cuentas registradas para este cliente.");
                return;
            }

            System.out.println("Número de cuenta | Saldo");
            System.out.println("-------------------------|----------");

            cuentasCliente.forEach(cuenta ->
                    System.out.printf(
                            "%s | %s%n",
                            cuenta.getNumeroCuenta(),
                            formatearImporte(cuenta.getSaldoCuenta())
                    )
            );
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            System.err.println("ERROR: El valor debe ser numérico.");
            entrada.nextLine();
        }
    }

    public void verInfoCuenta() {
        try {
            System.out.print("Introduzca número de cuenta: ");
            String numeroCuenta = entrada.nextLine().trim().toUpperCase();

            Cuenta cuenta = cuentaServicio.buscarNumero(numeroCuenta);

            System.out.println("Número de cuenta: " + cuenta.getNumeroCuenta());
            System.out.println(
                    "Titular: " + cuenta.getDueñoCuenta().getNombreCliente()
                            + " " + cuenta.getDueñoCuenta().getApellidosCliente()
            );
            System.out.println("Saldo: " + formatearImporte(cuenta.getSaldoCuenta()));
            System.out.println(
                    "Fecha de creación: " + cuenta.getFechaCreacionCuenta().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    )
            );
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        }
    }

    private String formatearImporte(BigDecimal importe) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "ES"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');

        DecimalFormat formato = new DecimalFormat("#,##0.00", symbols);
        return formato.format(importe) + " €";
    }
}