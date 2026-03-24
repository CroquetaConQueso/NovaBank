package org.example.repositorio;

import org.example.modelos.Cliente;

import java.util.HashMap;

public class RepositorioCliente {
    private HashMap<Long, Cliente> registroClientes = new HashMap<>();

    public void anadirCliente(Cliente nuevoCliente){
        if (!registroClientes.containsKey(nuevoCliente.getIdCliente())) {
            registroClientes.put(nuevoCliente.getIdCliente(), nuevoCliente);
        }
    }

    /* En el caso de que se pueda establecer
    public void eliminarCliente(Cliente antiguoCliente){
        registroClientes.remove(antiguoCliente.getIdCliente());
    }*/

    public void listarClientes(){
        registroClientes.forEach((a,b)-> System.out.println(a+ " | "+b.getNombreCliente()+" | "+b.getDniNifCliente()+"| "+b.getEmailCliente()+" | "+b.getTelefonoCliente()));
    }

    public Cliente buscarIdCliente(Long idBusqueda){
        return registroClientes.get(idBusqueda);
    }

    public Cliente buscarDniCliente(String dniNif){
        return registroClientes.values().stream().filter(b->b.getDniNifCliente().equals(dniNif))
                .findFirst().orElse(null);
    }

    public Cliente buscarEmailCliente(String email){
        return registroClientes.values().stream().filter(b->b.getEmailCliente().equals(email))
                .findFirst().orElse(null);
    }
    
    public Cliente buscarTelefonoCliente(int telefonoCli){
        return registroClientes.values().stream().filter(b->b.getTelefonoCliente() == telefonoCli)
                .findFirst().orElse(null);
    }
}
