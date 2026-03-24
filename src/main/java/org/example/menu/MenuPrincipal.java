package org.example.menu;

import java.util.Scanner;

public class MenuPrincipal {

    private Scanner entrada;
    private MenuCliente menuCli;
    private MenuCuenta menuCu;
    private MenuMovimientos menuMov;
    private MenuConsultas menuCo;

    public MenuPrincipal(Scanner entrada, MenuCliente menuCli, MenuCuenta menuCu,
                         MenuMovimientos menuMov, MenuConsultas menuCo) {
        this.entrada = entrada;
        this.menuCli = menuCli;
        this.menuCu = menuCu;
        this.menuMov = menuMov;
        this.menuCo = menuCo;
    }

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
                        menuCo.menuConsultas();
                        break;
                    case 5:
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
