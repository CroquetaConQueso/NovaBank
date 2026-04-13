package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.exception.DuplicateResourceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.ClienteRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de clientes.
 *
 * Aquí vive la lógica de negocio relacionada con validación de datos,
 * control de duplicados y coordinación con el repositorio.
 */
public class ClienteServicio {

    private final ClienteRepository repoCliente;

    public ClienteServicio(ClienteRepository repoCliente) {
        this.repoCliente = repoCliente;
    }

    public Cliente registrarCliente(String nombre, String apellidos, String dni, String email, int telefono) {
        validarNombre(nombre);
        validarApellidos(apellidos);

        String dniNormalizado = normalizarDni(dni);
        String emailNormalizado = normalizarEmail(email);

        validarDni(dniNormalizado);
        validarEmail(emailNormalizado);
        validarTelefono(telefono);

        if (repoCliente.buscarDniCliente(dniNormalizado) != null) {
            throw new DuplicateResourceException("Ya existe un cliente con el DNI " + dniNormalizado);
        }

        if (repoCliente.buscarEmailCliente(emailNormalizado) != null) {
            throw new DuplicateResourceException("Ya existe un cliente con el email " + emailNormalizado);
        }

        if (repoCliente.buscarTelefonoCliente(telefono) != null) {
            throw new DuplicateResourceException("Ya existe un cliente con el teléfono " + telefono);
        }

        Cliente nuevoCliente = Cliente.builder()
                .nombreCliente(nombre.trim())
                .apellidosCliente(apellidos.trim())
                .dniNifCliente(dniNormalizado)
                .emailCliente(emailNormalizado)
                .telefonoCliente(telefono)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        repoCliente.anadirCliente(nuevoCliente);
        return nuevoCliente;
    }

    public Cliente buscarIdCliente(Long idBusqueda) {
        if (idBusqueda == null || idBusqueda <= 0) {
            throw new ValidationException("Debes introducir una ID correcta");
        }

        Cliente cliente = repoCliente.buscarIdCliente(idBusqueda);

        if (cliente == null) {
            throw new ResourceNotFoundException("No existe ningún cliente con la ID " + idBusqueda);
        }

        return cliente;
    }

    public Cliente buscarDniCliente(String dni) {
        String dniNormalizado = normalizarDni(dni);
        validarDni(dniNormalizado);

        Cliente cliente = repoCliente.buscarDniCliente(dniNormalizado);

        if (cliente == null) {
            throw new ResourceNotFoundException("No existe ningún cliente con el DNI/NIF indicado");
        }

        return cliente;
    }

    public List<Cliente> listarClientes() {
        return repoCliente.obtenerClientes();
    }

    private String normalizarDni(String dni) {
        if (dni == null) {
            return null;
        }
        return dni.trim().toUpperCase();
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() < 2) {
            throw new ValidationException("El cliente debe de tener un nombre válido.");
        }

        for (char c : nombre.trim().toCharArray()) {
            if (!Character.isLetter(c) && !Character.isWhitespace(c)) {
                throw new ValidationException("El nombre solo puede contener caracteres alfabéticos.");
            }
        }
    }

    private void validarApellidos(String apellidos) {
        if (apellidos == null || apellidos.isBlank() || apellidos.trim().length() < 2) {
            throw new ValidationException("El cliente debe de tener apellidos válidos.");
        }

        for (char c : apellidos.trim().toCharArray()) {
            if (!Character.isLetter(c) && !Character.isWhitespace(c)) {
                throw new ValidationException("Los apellidos solo pueden contener letras y espacios.");
            }
        }
    }

    private void validarDni(String dni) {
        if (dni == null || dni.isBlank() || dni.length() != 9) {
            throw new ValidationException("El DNI/NIF debe tener 9 caracteres.");
        }

        for (int i = 0; i < 8; i++) {
            if (!Character.isDigit(dni.charAt(i))) {
                throw new ValidationException("Los primeros 8 caracteres del DNI/NIF deben ser numéricos.");
            }
        }

        if (!Character.isLetter(dni.charAt(8))) {
            throw new ValidationException("El último carácter del DNI/NIF debe ser una letra.");
        }
    }

    private void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("El email debe de tener un formato válido.");
        }

        if (email.length() > 254) {
            throw new ValidationException("El email no puede superar los 254 caracteres.");
        }

        int primerArroba = email.indexOf('@');
        int ultimaArroba = email.lastIndexOf('@');

        if (primerArroba <= 0 || primerArroba != ultimaArroba || primerArroba == email.length() - 1) {
            throw new ValidationException("El email debe contener un único '@' en una posición válida.");
        }

        String parteLocal = email.substring(0, primerArroba);
        String dominio = email.substring(primerArroba + 1);

        validarParteLocalEmail(parteLocal);
        validarDominioEmail(dominio);
    }

    private void validarParteLocalEmail(String parteLocal) {
        if (parteLocal.length() > 64) {
            throw new ValidationException("La parte local del email no puede superar los 64 caracteres.");
        }

        if (parteLocal.startsWith(".") || parteLocal.endsWith(".") || parteLocal.contains("..")) {
            throw new ValidationException("La parte local del email tiene un formato inválido.");
        }

        if (!parteLocal.matches("[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+")) {
            throw new ValidationException("La parte local del email contiene caracteres no permitidos.");
        }
    }

    private void validarDominioEmail(String dominio) {
        if (dominio.length() > 253) {
            throw new ValidationException("El dominio del email es demasiado largo.");
        }

        if (dominio.startsWith(".") || dominio.endsWith(".") || dominio.contains("..")) {
            throw new ValidationException("El dominio del email tiene un formato inválido.");
        }

        if (!dominio.contains(".")) {
            throw new ValidationException("El dominio del email debe contener al menos un punto.");
        }

        String[] etiquetas = dominio.split("\\.");

        for (String etiqueta : etiquetas) {
            if (etiqueta.isBlank()) {
                throw new ValidationException("El dominio del email contiene etiquetas vacías.");
            }

            if (etiqueta.length() > 63) {
                throw new ValidationException("Una etiqueta del dominio del email supera los 63 caracteres.");
            }

            if (etiqueta.startsWith("-") || etiqueta.endsWith("-")) {
                throw new ValidationException("Las etiquetas del dominio no pueden empezar ni terminar con guion.");
            }

            if (!etiqueta.matches("[A-Za-z0-9-]+")) {
                throw new ValidationException("El dominio del email contiene caracteres no permitidos.");
            }
        }

        String tld = etiquetas[etiquetas.length - 1];

        if (!tld.matches("[A-Za-z]{2,63}")) {
            throw new ValidationException("El dominio del email debe terminar en una extensión válida.");
        }
    }

    private void validarTelefono(int telefono) {
        if (String.valueOf(telefono).length() != 9) {
            throw new ValidationException("El teléfono debe de tener exactamente 9 dígitos.");
        }
    }
}