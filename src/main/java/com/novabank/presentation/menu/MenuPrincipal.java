package com.novabank.presentation.menu;

import java.util.Scanner;

/**
 * Menú principal de la aplicación.
 *
 * Se encarga de mostrar las opciones principales y delegar en los menús
 * especializados de clientes, cuentas, operaciones y consultas.
 */
public class MenuPrincipal {

    private final Scanner entrada;
    private final MenuCliente menuCli;
    private final MenuCuenta menuCu;
    private final MenuMovimientos menuMov;
    private final MenuConsulta menuCo;

    /**
     * Construye el menú principal con sus dependencias de presentación.
     *
     * @param entrada scanner compartido de entrada por consola
     * @param menuCli menú de gestión de clientes
     * @param menuCu menú de gestión de cuentas
     * @param menuMov menú de operaciones financieras
     * @param menuCo menú de consultas
     */
    public MenuPrincipal(Scanner entrada,
                         MenuCliente menuCli,
                         MenuCuenta menuCu,
                         MenuMovimientos menuMov,
                         MenuConsulta menuCo) {
        this.entrada = entrada;
        this.menuCli = menuCli;
        this.menuCu = menuCu;
        this.menuMov = menuMov;
        this.menuCo = menuCo;
    }

    /**
     * Muestra el menú principal y redirige a la opción seleccionada.
     */
    public void menuPrincipal() {
        while (true) {
            System.out.println();
            System.out.println("====================================");
            System.out.println("   NOVABANK - SISTEMA DE OPERACIONES");
            System.out.println("====================================");
            System.out.println("1. Gestión de clientes");
            System.out.println("2. Gestión de cuentas");
            System.out.println("3. Operaciones financieras");
            System.out.println("4. Consultas");
            System.out.println("5. Salir");
            System.out.print("Seleccione una opción: ");

            try {
                int opcion = Integer.parseInt(entrada.nextLine().trim());

                switch (opcion) {
                    case 1 -> menuCli.menuClientes();
                    case 2 -> menuCu.menuCuentas();
                    case 3 -> menuMov.menuMovimientos();
                    case 4 -> menuCo.menuConsultas();
                    case 5 -> {
                        System.out.println("Saliendo del sistema...");
                        return;
                    }
                    default -> System.out.println("ERROR: Debes escoger una opción válida del menú.");
                }
            } catch (NumberFormatException ex) {
                System.out.println("ERROR: Debes introducir un valor numérico.");
            }
        }
    }
}