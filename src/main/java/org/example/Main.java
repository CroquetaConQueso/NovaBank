package org.example;

import org.example.menu.MenuCliente;
import org.example.menu.MenuCuenta;
import org.example.menu.MenuMovimientos;
import org.example.repositorio.RepositorioCliente;
import org.example.repositorio.RepositorioCuenta;
import org.example.repositorio.RepositorioMovimiento;
import org.example.servicios.ClienteServicio;
import org.example.servicios.CuentaServicio;
import org.example.servicios.MovimientoServicio;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);

        RepositorioCliente repoCliente = new RepositorioCliente();
        RepositorioCuenta repoCuenta = new RepositorioCuenta();
        RepositorioMovimiento repoMovimiento = new RepositorioMovimiento();

        ClienteServicio cliServ = new ClienteServicio(repoCliente);
        CuentaServicio cuServ = new CuentaServicio(repoCuenta, repoCliente);
        MovimientoServicio moviServ = new MovimientoServicio(repoCuenta, repoMovimiento);

        MenuCliente menuCli = new MenuCliente(cliServ, entrada);
        MenuCuenta menuCu = new MenuCuenta(cuServ, entrada);
        MenuMovimientos menuMov = new MenuMovimientos(moviServ, entrada);

        while (true) {
            System.out.println();
            System.out.println("====================================");
            System.out.println("   NOVABANK - SISTEMA DE OPERACIONES");
            System.out.println("====================================");
            System.out.println("1. Gestión de clientes");
            System.out.println("2. Gestión de cuentas");
            System.out.println("3. Operaciones financieras");
            System.out.println("4. Salir");
            System.out.print("Seleccione una opción: ");

            try {
                int opcion = Integer.parseInt(entrada.nextLine().trim());

                switch (opcion) {
                    case 1:
                        menuCli.menuClientes();
                        break;
                    case 2:
                        menuCu.menuCuentas();
                        break;
                    case 3:
                        menuMov.menuMovimientos();
                        break;
                    case 4:
                        System.out.println("Saliendo del sistema...");
                        entrada.close();
                        return;
                    default:
                        System.out.println("ERROR: Debes escoger una opción válida del menú.");
                }

            } catch (NumberFormatException ex) {
                System.out.println("ERROR: Debes introducir un valor numérico.");
            }
        }
    }
}