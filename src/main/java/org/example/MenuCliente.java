package org.example;

import lombok.AllArgsConstructor;
import org.example.modelos.Cliente;
import org.example.servicios.ClienteServicio;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

@AllArgsConstructor
public class MenuCliente {
    private final ClienteServicio clienteServicio;
    private Scanner entrada;


    public void buscarClienteId(){
        try{
            System.out.print("Introduzca la ID del cliente: ");
            Long idBuscar = entrada.nextLong();

            Cliente cli = clienteServicio.getRepoCliente().buscarIdCliente(idBuscar);
        }catch(IllegalArgumentException ex){
            System.out.println("Debes de introducir una ID correcta");
        }
    }

    public void buscarClienteDni(){
        try{
            System.out.print("Introduzca el dni/nif del cliente: ");
            String dniBuscar = entrada.nextLine();

            Cliente cli = clienteServicio.getRepoCliente().buscarDniCliente(dniBuscar);
        }catch(IllegalArgumentException ex){
            System.out.println("Debes de introducir un DNI/NIF correcto");
        }
    }

    private void listarClientes() {
        System.out.println("\n--- LISTADO DE CLIENTES ---");
        System.out.println("ID    | Nombre               | DNI        | Email                        | Teléfono");
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

            clienteServicio.registrarCliente(nombreCliente,apellidosCliente,dniNifCliente,emailCliente,telefonoCliente);

        }catch(IllegalArgumentException ex){
            System.out.println("Error: "+ex.getMessage());
        }
    }

    public void menuClientes(){
        while(true){
            System.out.println();
            System.out.println("====================================");
            System.out.println("         GESTIÓN DE CLIENTES");
            System.out.println("====================================");
            System.out.println("1. Registrar cliente");
            System.out.println("2. Buscar cliente por ID");
            System.out.println("3. Buscar cliente por DNI");
            System.out.println("4. Listar clientes");
            System.out.println("5. Volver");
            System.out.print("Seleccione una opción: ");

            try {
                int respuestaSwitch = entrada.nextInt();

                switch (respuestaSwitch){
                    case 1:
                        registrarCliente();
                        break;
                    case 2:
                        buscarClienteId();
                        break;
                    case 3:
                        buscarClienteDni();
                        break;
                    case 4:
                        listarClientes();
                        break;
                    case 5:
                        System.out.println("Volviendo al menú principal...");
                        return;
                    default:
                        System.out.println("Debes de escoger una opción encontrada en el menu");
                }
            }catch (IllegalArgumentException ex){
                System.err.println("Debes de introducir un valor numérico");
            }
        }
    }
}
