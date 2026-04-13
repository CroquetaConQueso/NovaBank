package com.novabank.presentation.menu;

import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.exception.NovaBankException;
import com.novabank.service.CuentaServicio;
import com.novabank.service.MovimientoServicio;
import com.novabank.util.Utilidades;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Menú de consultas.
 *
 * Gestiona la interacción por consola para consultar saldo e historial
 * de movimientos, delegando la lógica en los servicios.
 */
public class MenuConsulta {

    private final CuentaServicio cuSer;
    private final MovimientoServicio moSer;
    private final Scanner entrada;

    public MenuConsulta(CuentaServicio cuSer, MovimientoServicio moSer, Scanner entrada) {
        this.cuSer = cuSer;
        this.moSer = moSer;
        this.entrada = entrada;
    }

    private LocalDate validacionFecha(String mensaje, DateTimeFormatter formato) {
        while (true) {
            try {
                System.out.print(mensaje);
                String entradaFecha = entrada.nextLine().trim();
                return LocalDate.parse(entradaFecha, formato);
            } catch (Exception ex) {
                System.err.println("Formato inválido. El valor debe de tener la estructura: yyyy-MM-dd");
            }
        }
    }

    private String tomarNumeroCuenta() {
        System.out.print("Introduzca número de cuenta: ");
        String numeroCuenta = entrada.nextLine().trim().toUpperCase();

        if (!Utilidades.validarNumeroCuenta(numeroCuenta)) {
            throw new NovaBankException("El número de cuenta debe tener formato ES seguido de 20 dígitos");
        }

        return numeroCuenta;
    }

    public void historiaRangoFechas() {
        try {
            String numeroCuenta = tomarNumeroCuenta();
            cuSer.buscarNumero(numeroCuenta);

            DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate feIni = validacionFecha("Fecha inicio (yyyy-MM-dd): ", formato);
            LocalDate feFin = validacionFecha("Fecha fin (yyyy-MM-dd): ", formato);

            List<Movimiento> movimientosRango = moSer.obtenerListaFecha(numeroCuenta, feIni, feFin);

            System.out.println("Movimientos del " + feIni + " al " + feFin + ":");

            if (movimientosRango.isEmpty()) {
                System.out.println("No hay movimientos en el rango de fechas indicado.");
            } else {
                System.out.println("Fecha                | Tipo                        | Cantidad");
                System.out.println("---------------------|-----------------------------|------------");

                for (Movimiento movimiento : movimientosRango) {
                    System.out.printf("%s | %-27s | %10.2f €%n",
                            movimiento.getFechaCreacionMov().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            movimiento.getTipoMov(),
                            movimiento.getCantidadMovimiento());
                }
            }
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        }
    }

    public void consultarSaldo() {
        try {
            String numeroCuenta = tomarNumeroCuenta();
            Cuenta cuenta = cuSer.buscarNumero(numeroCuenta);

            System.out.println("Saldo actual: " + cuenta.getSaldoCuenta() + " €");
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        }
    }

    public void historialMovimientos() {
        try {
            String numeroCuenta = tomarNumeroCuenta();
            cuSer.buscarNumero(numeroCuenta);

            List<Movimiento> listaMovimientos = moSer.obtenerLista(numeroCuenta);

            if (listaMovimientos.isEmpty()) {
                System.out.println("No hay movimientos registrados para esta cuenta");
            } else {
                System.out.println("Historial de movimientos - " + numeroCuenta + ":");
                System.out.println("Fecha                | Tipo                        | Cantidad");
                System.out.println("---------------------|-----------------------------|------------");

                for (Movimiento movimiento : listaMovimientos) {
                    System.out.printf("%s | %-27s | %10.2f €%n",
                            movimiento.getFechaCreacionMov().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            movimiento.getTipoMov(),
                            movimiento.getCantidadMovimiento());
                }
            }
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        }
    }

    public void menuConsultas() {
        while (true) {
            System.out.println();
            System.out.println("--- CONSULTAS ---");
            System.out.println("1. Consultar saldo");
            System.out.println("2. Historial de movimientos");
            System.out.println("3. Movimientos por rango de fechas");
            System.out.println("4. Volver");
            System.out.print("Seleccione una opción: ");

            try {
                int opcionSwitch = Integer.parseInt(entrada.nextLine().trim());

                switch (opcionSwitch) {
                    case 1 -> consultarSaldo();
                    case 2 -> historialMovimientos();
                    case 3 -> historiaRangoFechas();
                    case 4 -> {
                        System.out.println("Volviendo al menú principal...");
                        return;
                    }
                    default -> System.out.println("Opción no válida.");
                }
            } catch (NumberFormatException ex) {
                System.err.println("ERROR: Debes introducir un valor numérico.");
            }
        }
    }
}