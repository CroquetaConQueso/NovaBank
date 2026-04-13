package com.novabank;

import com.novabank.persistence.memory.RepositorioCliente;
import com.novabank.persistence.memory.RepositorioCuenta;
import com.novabank.persistence.memory.RepositorioMovimiento;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;
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
 */
public class Main {

    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);

        ClienteRepository repoCliente = new RepositorioCliente();
        CuentaRepository repoCuenta = new RepositorioCuenta();
        MovimientoRepository repoMovimiento = new RepositorioMovimiento();

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