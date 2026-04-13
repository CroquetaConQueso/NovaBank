package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.persistence.memory.RepositorioCliente;

import java.time.LocalDateTime;

/**
 * Servicio encargado de la lógica de negocio relacionada con los clientes.
 *
 * Gestiona las validaciones, reglas de unicidad y operaciones
 * necesarias antes de interactuar con el repositorio.
 *
 * Actúa como intermediario entre la capa de presentación
 * y la capa de persistencia en memoria.
 */
public class ClienteServicio {

    private RepositorioCliente repoCliente;

    public ClienteServicio(RepositorioCliente repoCliente) {
        this.repoCliente = repoCliente;
    }

    public RepositorioCliente getRepoCliente() {
        return repoCliente;
    }

    /**
     * Dentro de este bloque hasta el siguiente comentario se encuentran las validaciones,
     * en estas podemos encontrar las validaciones de:
     *  -nombre
     *  -apellido
     *  -dni
     *  -telefono
     *  -email
     *  -telefono
     */
    private void validarNombre(String nombre){
        if(nombre.isEmpty() || nombre.isBlank()){
            throw new IllegalArgumentException("El cliente debe de tener un nombre");
        }else if(nombre.length() < 2){
            throw new IllegalArgumentException("El nombre debe de tener más de dos carácteres");
        }

        for (int i = 0; i < nombre.length() ; i++) {
            char valor = nombre.charAt(i);
            if(!Character.isAlphabetic(valor)){
                throw new IllegalArgumentException("Los caracteres solo pueden ser alfabéticos");
            }
        }
    }

    private void validarApellidos(String apellidos){
        if(apellidos.isEmpty() || apellidos.isBlank()){
            throw new IllegalArgumentException("El cliente debe de tener apellidos");
        }else if(apellidos.length() < 2){
            throw new IllegalArgumentException("Los apellidos debe de tener más de dos carácteres");
        }

        for (int i = 0; i < apellidos.length(); i++) {
            char valor = apellidos.charAt(i);
            if (!Character.isAlphabetic(valor) && !Character.isWhitespace(valor)) {
                throw new IllegalArgumentException("Los caracteres solo pueden ser alfabéticos");
            }
        }
    }

    private void validarDni(String dniNif){
        if(dniNif.isBlank() || dniNif.isEmpty()){
            throw new IllegalArgumentException("El cliente debe de tener un dni/nif");
        }else if(dniNif.length() != 9){
            throw new IllegalArgumentException("El tamaño del dni/nif debe de ser 9 carácteres");
        }


        for (int i = 0; i < 8; i++) {
            if (!Character.isDigit(dniNif.charAt(i))) {
                throw new IllegalArgumentException("Los primeros 8 caracteres del dni/nif deben de ser numéricos");
            }
        }

        if (!Character.isLetter(dniNif.charAt(8))) {
            throw new IllegalArgumentException("El último carácter del dni/nif debe de ser una letra");
        }

    }

    private void validarEmail(String email) {
        if(email.isEmpty() || email.isBlank()){
            throw new IllegalArgumentException("El cliente debe de tener un email");
        }else if(!email.matches(".+@.+\\..+")){
            throw new IllegalArgumentException("El email debe de contener un formato adecuado");
        }
    }

    private void validarTelefono(int telefonoCliente){
        if(telefonoCliente <= 0){
            throw new IllegalArgumentException("El numero de telefono no puede ser negativo");
        }else if(String.valueOf(telefonoCliente).length() !=9){
            throw new IllegalArgumentException("El numero de telefono debe de tener 9 caracteres");
        }
    }

    //LOGICA

    public void listarClientes(){
        repoCliente.listarClientes();
    }

    /**
     * Busca un cliente por su identificador,
     * validando previamente que sea un valor correcto.
     */

    public Cliente buscarIdCliente(Long idBuscar) {
        if (idBuscar == null || idBuscar <= 0) {
            throw new IllegalArgumentException("Debes de introducir una ID correcta");
        }

        Cliente cliente = repoCliente.buscarIdCliente(idBuscar);

        if (cliente == null) {
            throw new IllegalArgumentException("No se ha podido encontrar al cliente con esa ID");
        }

        return cliente;
    }

    // Busca un cliente por su DNI/NIF tras validar su formato.
    public Cliente buscarDniCliente(String dniBuscar) {
        validarDni(dniBuscar);

        Cliente cliente = repoCliente.buscarDniCliente(dniBuscar);

        if (cliente == null) {
            throw new IllegalArgumentException("No se ha podido encontrar al cliente con ese dni");
        }

        return cliente;
    }

    /**
     * Registra un nuevo cliente en el sistema.
     *
     * Aplica todas las validaciones necesarias y verifica
     * que DNI, email y teléfono sean únicos antes de crear
     * el objeto Cliente y almacenarlo en el repositorio.
     *
     * @param nombreCliente nombre del cliente
     * @param apellidosCliente apellidos del cliente
     * @param dniNifCliente DNI/NIF del cliente
     * @param emailCliente correo electrónico
     * @param telefonoCliente número de teléfono
     */
    public void registrarCliente(String nombreCliente,String apellidosCliente, String dniNifCliente, String emailCliente, int telefonoCliente ){

        validarNombre(nombreCliente);
        validarApellidos(apellidosCliente);

        validarDni(dniNifCliente);
        if(repoCliente.buscarDniCliente(dniNifCliente)!=null){
            throw new IllegalArgumentException("Ya existe un cliente con el DNI "+dniNifCliente);
        }

        validarEmail(emailCliente);
        if(repoCliente.buscarEmailCliente(emailCliente)!=null){
            throw new IllegalArgumentException("Ya existe un cliente con el email "+emailCliente);
        }

        validarTelefono(telefonoCliente);
        if(repoCliente.buscarTelefonoCliente(telefonoCliente)!= null){
            throw new IllegalArgumentException("Ya existe un cliente con el telefono "+telefonoCliente);
        }
        Cliente nuevoCliente = new Cliente(nombreCliente,apellidosCliente,dniNifCliente,emailCliente,telefonoCliente, LocalDateTime.now());

        repoCliente.anadirCliente(nuevoCliente);
        System.out.println("Cliente creado correctamente.");
        System.out.println("ID cliente: "+nuevoCliente.getIdCliente());
    }

}
