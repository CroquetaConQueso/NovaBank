package com.novabank;

import com.novabank.persistence.memory.RepositorioCliente;
import com.novabank.persistence.memory.RepositorioCuenta;
import com.novabank.persistence.memory.RepositorioMovimiento;
import com.novabank.presentation.menu.MenuCliente;
import com.novabank.presentation.menu.MenuConsulta;
import com.novabank.presentation.menu.MenuCuenta;
import com.novabank.presentation.menu.MenuMovimientos;
import com.novabank.presentation.menu.MenuPrincipal;
import com.novabank.service.ClienteServicio;
import com.novabank.service.CuentaServicio;
import com.novabank.service.MovimientoServicio;

import java.util.Scanner;

/**
 * Punto de entrada de la aplicación NovaBank.
 *
 * En este issue solo se actualiza su ubicación al nuevo paquete raíz y
 * se corrigen los imports para respetar la arquitectura por capas.
 */
public class Main {

    /**
     * Arranca la aplicación de consola y cablea sus dependencias en memoria.
     *
     * @param args argumentos de arranque de la aplicación
     */
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
        MenuConsulta menuCo = new MenuConsulta(cuServ, moviServ, entrada);
        MenuPrincipal menuPri = new MenuPrincipal(entrada, menuCli, menuCu, menuMov, menuCo);

        menuPri.menuPrincipal();
        entrada.close();
    }
}