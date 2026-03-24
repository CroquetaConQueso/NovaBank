package org.example;

import org.example.menu.*;
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
        MenuConsultas menuCo = new MenuConsultas(cuServ,moviServ,entrada);
        MenuPrincipal menuPri = new MenuPrincipal(entrada,menuCli,menuCu,menuMov, menuCo);

        menuPri.menuPrincipal();

        entrada.close();
    }
}