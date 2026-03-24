package org.example.menu;

import lombok.AllArgsConstructor;
import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.servicios.CuentaServicio;
import org.example.servicios.MovimientoServicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

@AllArgsConstructor
public class MenuConsultas {
    private CuentaServicio cuSer;
    private MovimientoServicio moSer;
    private Scanner entrada;

    //Organización por fecha
    public void historiaRangoFechas(){
        try {
            System.out.print("Introduzca número de cuenta: ");
            String numeroCuenta = entrada.nextLine().trim().toUpperCase();

            Cuenta cuenta = cuSer.buscarNumero(numeroCuenta);
            System.out.print("Fecha inicio (yyyy-MM-dd): ");
            String feIn = entrada.nextLine();
            System.out.print("Fecha fin (yyyy-MM-dd): ");
            String feFin = entrada.nextLine();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate feIni = LocalDate.parse(feIn,fmt);
            LocalDate feFina = LocalDate.parse(feFin,fmt);

            List<Movimiento> lista = moSer.obtenerListaFecha(numeroCuenta, feIni, feFina);
            System.out.println("Movimientos del "+feIni+" al "+feFina+":");
            System.out.println("\nFecha               | Tipo                   | Cantidad"+
                    "\n--------------------|------------------------|------------");
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
        }catch(IllegalArgumentException ex){
            System.err.println("ERROR: "+ex.getMessage());
        }

    }
    public void historialMov(){
        try{
            System.out.print("Introduzca número de cuenta: ");
            String numeroCuenta = entrada.nextLine().trim().toUpperCase();

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
        System.out.print("Introduzca número de cuenta: ");
        String numeroCuenta = entrada.nextLine().trim().toUpperCase();

        Cuenta cuenta = cuSer.buscarNumero(numeroCuenta);
        System.out.println("Saldo actual: "+cuenta.getSaldoCuenta()+" €");
    }

    public void menuConsultas(){
        while(true) {

                System.out.println("--- CONSULTAS ---");
                System.out.println("1.Consultar Saldo");
                System.out.println("2.Historial de movimientos");
                System.out.println("3.Movimientos por rango de fechas");
                System.out.println("4.Volver");

                System.out.println("Seleccione una opción: ");
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
            }catch(IllegalArgumentException ex){
                System.err.println("Debes de introducir un valor numérico");
            }
        }
    }
}