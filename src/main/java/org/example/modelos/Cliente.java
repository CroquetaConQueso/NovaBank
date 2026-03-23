package org.example.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Cliente {
    private static long contadorIds = 1000L;

    private long idCliente = ++contadorIds;
    private String nombreCliente;
    private String apellidosCliente;
    private String dniNifCliente;
    private String emailCliente;
    private int telefonoCliente;
    private LocalDateTime fechaCreacionCliente;

    public Cliente(String nombreCliente, String apellidosCliente, String dniNifCliente,
                   String emailCliente, int telefonoCliente, LocalDateTime fechaCreacionCliente) {
        this.nombreCliente = nombreCliente;
        this.apellidosCliente = apellidosCliente;
        this.dniNifCliente = dniNifCliente;
        this.emailCliente = emailCliente;
        this.telefonoCliente = telefonoCliente;
        this.fechaCreacionCliente = fechaCreacionCliente;
    }

    @Override
    public String toString() {
        return "Cliente encontrado: \nID: " + idCliente +
                "\nNombre: " + nombreCliente + " " + apellidosCliente +
                "\nDNI: " + dniNifCliente +
                "\nEmail: " + emailCliente +
                "\nTeléfono: " + telefonoCliente;
    }
}
