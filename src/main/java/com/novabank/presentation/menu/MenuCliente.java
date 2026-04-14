package com.novabank.presentation.menu;

import com.novabank.domain.model.Cliente;
import com.novabank.exception.NovaBankException;
import com.novabank.service.ClienteServicio;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Menú de clientes.
 *
 * Su responsabilidad es gestionar la interacción por consola y delegar
 * la lógica de negocio en ClienteServicio.
 */
public class MenuCliente {

    private final ClienteServicio clienteServicio;
    private final Scanner entrada;

    public MenuCliente(ClienteServicio clienteServicio, Scanner entrada) {
        this.clienteServicio = clienteServicio;
        this.entrada = entrada;
    }

    public void buscarClienteId() {
        try {
            System.out.print("Introduzca la ID del cliente: ");
            Long idBuscar = entrada.nextLong();
            entrada.nextLine();

            Cliente cliente = clienteServicio.buscarIdCliente(idBuscar);

            System.out.println("Cliente encontrado:");
            System.out.println("ID: " + cliente.getIdCliente());
            System.out.println("Nombre: " + cliente.getNombreCliente() + " " + cliente.getApellidosCliente());
            System.out.println("DNI: " + cliente.getDniNifCliente());
            System.out.println("Email: " + cliente.getEmailCliente());
            System.out.println("Teléfono: " + cliente.getTelefonoCliente());
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            System.err.println("ERROR: Debes introducir un valor numérico.");
            entrada.nextLine();
        }
    }

    public void buscarClienteDni() {
        try {
            System.out.print("Introduzca el DNI/NIF del cliente: ");
            String dniBuscar = entrada.nextLine().trim().toUpperCase();

            Cliente cliente = clienteServicio.buscarDniCliente(dniBuscar);

            System.out.println("Cliente encontrado:");
            System.out.println("ID: " + cliente.getIdCliente());
            System.out.println("Nombre: " + cliente.getNombreCliente() + " " + cliente.getApellidosCliente());
            System.out.println("DNI: " + cliente.getDniNifCliente());
            System.out.println("Email: " + cliente.getEmailCliente());
            System.out.println("Teléfono: " + cliente.getTelefonoCliente());
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        }
    }

    public void buscarCliente() {
        System.out.println("Búsqueda de cliente:");
        System.out.println("1. Por DNI");
        System.out.println("2. Por ID");
        System.out.print("Seleccione una opción: ");

        try {
            int opcionSwitch = entrada.nextInt();
            entrada.nextLine();

            switch (opcionSwitch) {
                case 1 -> buscarClienteDni();
                case 2 -> buscarClienteId();
                default -> System.err.println("ERROR: Opción no válida.");
            }
        } catch (InputMismatchException ex) {
            System.err.println("ERROR: Debes introducir un valor numérico.");
            entrada.nextLine();
        }
    }

    private void listarClientes() {
        List<Cliente> clientes = clienteServicio.listarClientes();

        if (clientes.isEmpty()) {
            System.out.println("No hay clientes registrados en el sistema.");
            return;
        }

        System.out.println("ID | Nombre | DNI | Email | Teléfono");
        System.out.println("-----|------------------|------------|-----------------------------------|-----------");

        clientes.forEach(cliente -> {
            String nombreCompleto = cliente.getNombreCliente() + " " + cliente.getApellidosCliente();

            System.out.printf(
                    "%d | %s | %s | %s | %d%n",
                    cliente.getIdCliente(),
                    nombreCompleto,
                    cliente.getDniNifCliente(),
                    cliente.getEmailCliente(),
                    cliente.getTelefonoCliente()
            );
        });
    }

    public void registrarCliente() {
        try {
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

            Cliente cliente = clienteServicio.registrarCliente(
                    nombreCliente,
                    apellidosCliente,
                    dniNifCliente,
                    emailCliente,
                    telefonoCliente
            );

            System.out.println("Cliente creado correctamente.");
            System.out.println("ID cliente: " + cliente.getIdCliente());
        } catch (NovaBankException ex) {
            System.err.println("ERROR: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            System.err.println("ERROR: El teléfono debe ser numérico.");
            entrada.nextLine();
        }
    }

    public void menuClientes() {
        while (true) {
            System.out.println();
            System.out.println("--- GESTIÓN DE CLIENTES ---");
            System.out.println("1. Crear cliente");
            System.out.println("2. Buscar cliente");
            System.out.println("3. Listar clientes");
            System.out.println("4. Volver");
            System.out.print("Seleccione una opción: ");

            try {
                int respuestaSwitch = entrada.nextInt();
                entrada.nextLine();

                switch (respuestaSwitch) {
                    case 1 -> registrarCliente();
                    case 2 -> buscarCliente();
                    case 3 -> listarClientes();
                    case 4 -> {
                        System.out.println("Volviendo al menú principal...");
                        return;
                    }
                    default -> System.err.println("ERROR: Debes escoger una opción encontrada en el menú.");
                }
            } catch (InputMismatchException ex) {
                System.err.println("ERROR: Debes introducir un valor numérico.");
                entrada.nextLine();
            }
        }
    }
}