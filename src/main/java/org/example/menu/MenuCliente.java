package org.example.menu;

import org.example.modelos.Cliente;
import org.example.servicios.ClienteServicio;

import java.util.InputMismatchException;
import java.util.Scanner;

public class MenuCliente {

    private final ClienteServicio clienteServicio;
    private Scanner entrada;

    public MenuCliente(ClienteServicio clienteServicio, Scanner entrada) {
        this.clienteServicio = clienteServicio;
        this.entrada = entrada;
    }

    public void buscarClienteId(){
        try{
            System.out.print("Introduzca la ID del cliente: ");
            Long idBuscar = entrada.nextLong();
            entrada.nextLine();

            Cliente cli = clienteServicio.getRepoCliente().buscarIdCliente(idBuscar);
            System.out.println(cli);
        }catch(IllegalArgumentException ex){
            System.err.println("Debes de introducir una ID correcta");
        }catch(InputMismatchException inex){
            System.err.println("Debes de introducir un valor númerico");
            entrada.nextLine();
        }
    }

    public void buscarClienteDni(){
        try{
            System.out.print("Introduzca el dni/nif del cliente: ");
            String dniBuscar = entrada.nextLine().toUpperCase();

            Cliente cli = clienteServicio.getRepoCliente().buscarDniCliente(dniBuscar);
            System.out.println(cli);
        }catch(IllegalArgumentException ex){
            System.out.println("Debes de introducir un DNI/NIF correcto");
        }
    }

    public void buscarCliente() {
        System.out.println("Busqueda:");
        System.out.println("1.DNI");
        System.out.println("2.ID");
        System.out.print("Seleccione una opción: ");

        try {
            int opcionSwitch = entrada.nextInt();
            entrada.nextLine();

            switch (opcionSwitch){
                case 1:
                    buscarClienteDni();
                    break;
                case 2:
                    buscarClienteId();
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        } catch (InputMismatchException ex) {
            System.err.println("Error: Debes introducir un valor numérico");
            entrada.nextLine();
        }
    }

    private void listarClientes() {
        System.out.println("\n--- LISTADO DE CLIENTES ---");
        System.out.println("ID    | Nombre      | DNI        | Email          | Teléfono");
        clienteServicio.listarClientes();
    }

    public void registrarCliente(){
        try{
            System.out.print("Nombre: ");
            String nombreCliente = entrada.nextLine().trim();

            System.out.print("Apellidos: ");
            String apellidosCliente = entrada.nextLine().trim();

            System.out.print("DNI/NIF: ");
            String dniNifCliente = entrada.nextLine().trim().toUpperCase();

            System.out.print("Email: ");
            String emailCliente = entrada.nextLine().trim();

            System.out.print("Teléfono: ");
            int telefonoCliente = entrada.nextInt();
            entrada.nextLine();

            clienteServicio.registrarCliente(nombreCliente,apellidosCliente,dniNifCliente,emailCliente,telefonoCliente);
        }catch(IllegalArgumentException ex){
            System.out.println("Error: "+ex.getMessage());
        }catch(InputMismatchException inex){
            System.err.println("Error: El teléfono debe de ser numérico");
            entrada.nextLine();
        }
    }

    public void menuClientes(){
        while(true){
            System.out.println();
            System.out.println("====================================");
            System.out.println("         GESTIÓN DE CLIENTES");
            System.out.println("====================================");
            System.out.println("1. Registrar cliente");
            System.out.println("2. Buscar cliente");
            System.out.println("3. Listar clientes");
            System.out.println("4. Volver");
            System.out.print("Seleccione una opción: ");

            try {
                int respuestaSwitch = entrada.nextInt();
                entrada.nextLine();
                switch (respuestaSwitch){
                    case 1:
                        registrarCliente();
                        break;
                    case 2:
                        buscarCliente();
                        break;
                    case 3:
                        listarClientes();
                        break;
                    case 4:
                        System.out.println("Volviendo al menú principal...");
                        return;
                    default:
                        System.err.println("Debes de escoger una opción encontrada en el menu");
                }
            }catch (IllegalArgumentException ex){
                System.err.println("Debes de introducir un valor numérico");
            }catch (InputMismatchException ex){
                System.err.println("Error: "+ex.getMessage());
                entrada.nextLine();
            }
        }
    }
}
