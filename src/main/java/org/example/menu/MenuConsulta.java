package org.example.menu;

import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.servicios.CuentaServicio;
import org.example.servicios.MovimientoServicio;
import org.example.utilidades.Utilidades;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class MenuConsulta {

    private CuentaServicio cuSer;
    private MovimientoServicio moSer;
    private Scanner entrada;

    public MenuConsulta(CuentaServicio cuSer, MovimientoServicio moSer, Scanner entrada) {
        this.cuSer = cuSer;
        this.moSer = moSer;
        this.entrada = entrada;
    }

    /*
        Auxiliar que simplifica la validación de fecha, tomando el formato y devolviendo localdate
     */
    private LocalDate validacionFecha(String mensaje,DateTimeFormatter dmt) {
        while (true) {
            try {
                System.out.print(mensaje);
                String entradaFecha = entrada.nextLine().trim();
                return LocalDate.parse(entradaFecha, dmt);
            } catch (Exception e) {
                System.err.println("Formato inválido. El valor debe de tener la estructura: yyyy-MM-dd");
            }
        }
    }

    private String tomarNumeroCuenta(){
        System.out.print("Introduzca número de cuenta: ");
        String numeroCuenta = entrada.nextLine().trim().toUpperCase();

        if (!Utilidades.validarNumeroCuenta(numeroCuenta)) {
            throw new IllegalArgumentException(
                    "El número de cuenta debe tener formato ES seguido de 20 dígitos"
            );
        }
        return numeroCuenta;
    }

    /**
     * Muestra los movimientos de una cuenta dentro de un rango de fechas
     * introducidas por el usuario.
     */
    public void historiaRangoFechas(){
        try {
            String numeroCuenta = tomarNumeroCuenta();
            cuSer.buscarNumero(numeroCuenta);
            DateTimeFormatter dmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate feIni = validacionFecha("Fecha inicio (yyyy-MM-dd): ",dmt);
            LocalDate feFina = validacionFecha("Fecha fin (yyyy-MM-dd): ",dmt);

            List<Movimiento> lista = moSer.obtenerListaFecha(numeroCuenta, feIni, feFina);
            System.out.println("Movimientos del "+feIni+" al "+feFina+":");
            System.out.println("\nFecha               | Tipo                   | Cantidad"+
                    "\n--------------------|------------------------|------------");
            for (Movimiento mo : lista) {
                String formatoFe = mo.getFechaCreacionMov().format(dmt);

                String tipo;
                switch (mo.getTipoMov()) {
                    case DEPOSITO -> tipo = "Deposito";
                    case RETIRO -> tipo = "Retiro";
                    case TRANSFERENCIA_ENTRANTE -> tipo = "Transferencia Entrante";
                    case TRANSFERENCIA_SALIENTE -> tipo = "Transferencia Saliente";
                    default -> throw new IllegalArgumentException("El tipo debe de encontrarse entre los adecuados");
                }
                System.out.println(formatoFe + " | " + tipo + " | " + mo.getCantidadMovimiento() + " €");
            }
        }catch(IllegalArgumentException ex){
            System.err.println("ERROR: "+ex.getMessage());
        }

    }

    /**
     * Muestra el historial completo de movimientos de una cuenta específica.
     */
    public void historialMov(){
        try{
            String numeroCuenta = tomarNumeroCuenta();
            Cuenta cuenta = cuSer.buscarNumero(numeroCuenta);

            List<Movimiento> lista = moSer.obtenerLista(numeroCuenta);

            System.out.println("\n\nHistorial de movimientos - "+cuenta.getNumeroCuenta());
            System.out.println("Fecha               | Tipo                   | Cantidad");
            System.out.println("--------------------|------------------------|------------");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Movimiento mo : lista) {
                String formatoFe = mo.getFechaCreacionMov().format(fmt);

                String tipo;
                switch (mo.getTipoMov()) {
                    case DEPOSITO -> tipo = "Deposito";
                    case RETIRO -> tipo = "Retiro";
                    case TRANSFERENCIA_ENTRANTE -> tipo = "Transferencia Entrante";
                    case TRANSFERENCIA_SALIENTE -> tipo = "Transferencia Saliente";
                    default -> throw new IllegalArgumentException("El tipo debe de encontrarse entre los adecuados");
                }
                System.out.println(formatoFe + " | " + tipo + " | " + mo.getCantidadMovimiento() + " €");
            }
        }catch (IllegalArgumentException ex){
            System.err.println("ERROR: "+ex.getMessage());
        }
    }

    public void consultarSaldo(){
        try {
            String numeroCuenta = tomarNumeroCuenta();
            Cuenta cuenta = cuSer.buscarNumero(numeroCuenta);
            System.out.println("Saldo actual: " + cuenta.getSaldoCuenta() + " €");
        }catch(IllegalArgumentException ex){
            System.err.println("ERROR: "+ex.getMessage());
        }
    }

    /**
     * Muestra el menú interactivo de consultas y gestiona la navegación entre las distintas opciones.
     * Utiliza los métodos encontrados en la clase
     */

    public void menuConsultas(){
        while(true) {

                System.out.println("--- CONSULTAS ---");
                System.out.println("1.Consultar Saldo");
                System.out.println("2.Historial de movimientos");
                System.out.println("3.Movimientos por rango de fechas");
                System.out.println("4.Volver");

                System.out.print("Seleccione una opción: ");
            try{
                int opcionSwi = entrada.nextInt();
                entrada.nextLine();

                switch (opcionSwi) {
                    case 1:
                        consultarSaldo();
                        break;
                    case 2:
                        historialMov();
                        break;
                    case 3:
                        historiaRangoFechas();
                        break;
                    case 4:
                        System.out.println("Volviendo al menu principal");
                        return;
                    default:
                        System.err.println("Debes de escoger una opción encontrada en el menu");
                }
            }catch(InputMismatchException ex){
                System.err.println("Debes de introducir un valor numérico");
                entrada.nextLine();
            }
        }
    }
}