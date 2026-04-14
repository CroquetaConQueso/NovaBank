package com.novabank;

import com.novabank.config.RepositoryFactory;
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
import com.novabank.service.strategy.GeneradorNumeroCuentaAleatorio;
import com.novabank.service.strategy.GeneradorNumeroCuentaStrategy;

import java.util.Scanner;

/**
 * Punto de entrada de la aplicación NovaBank.
 */
public class Main {

    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);

        ClienteRepository repoCliente = RepositoryFactory.crearClienteRepository();
        CuentaRepository repoCuenta = RepositoryFactory.crearCuentaRepository();
        MovimientoRepository repoMovimiento = RepositoryFactory.crearMovimientoRepository();

        GeneradorNumeroCuentaStrategy generadorNumeroCuentaStrategy =
                new GeneradorNumeroCuentaAleatorio(repoCuenta);

        ClienteServicio cliServ = new ClienteServicio(repoCliente);
        CuentaServicio cuServ = new CuentaServicio(
                repoCuenta,
                repoCliente,
                generadorNumeroCuentaStrategy
        );

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