package org.example.modelos;

import java.time.LocalDateTime;

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

    // Getters

    public long getIdCliente() {
        return idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getApellidosCliente() {
        return apellidosCliente;
    }

    public String getDniNifCliente() {
        return dniNifCliente;
    }

    public String getEmailCliente() {
        return emailCliente;
    }

    public int getTelefonoCliente() {
        return telefonoCliente;
    }

    public LocalDateTime getFechaCreacionCliente() {
        return fechaCreacionCliente;
    }

    // Setters (kept to preserve @Data behavior)

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public void setApellidosCliente(String apellidosCliente) {
        this.apellidosCliente = apellidosCliente;
    }

    public void setDniNifCliente(String dniNifCliente) {
        this.dniNifCliente = dniNifCliente;
    }

    public void setEmailCliente(String emailCliente) {
        this.emailCliente = emailCliente;
    }

    public void setTelefonoCliente(int telefonoCliente) {
        this.telefonoCliente = telefonoCliente;
    }

    public void setFechaCreacionCliente(LocalDateTime fechaCreacionCliente) {
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
