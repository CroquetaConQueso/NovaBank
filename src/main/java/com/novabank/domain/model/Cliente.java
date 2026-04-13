package com.novabank.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa un cliente del sistema bancario.
 *
 * En este primer refactor se conserva el contrato actual de la clase para no
 * romper servicios ni tests, pero se elimina parte del código repetitivo con Lombok.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Cliente {

    private static long contadorIds = 1000L;

    private long idCliente;
    private String nombreCliente;
    private String apellidosCliente;
    private String dniNifCliente;
    private String emailCliente;
    private int telefonoCliente;
    private LocalDateTime fechaCreacionCliente;

    /**
     * Construye un cliente con sus datos funcionales actuales.
     *
     * @param nombreCliente nombre del cliente
     * @param apellidosCliente apellidos del cliente
     * @param dniNifCliente documento identificativo
     * @param emailCliente correo electrónico
     * @param telefonoCliente teléfono de contacto
     * @param fechaCreacionCliente fecha de creación
     */
    @Builder(toBuilder = true)
    public Cliente(String nombreCliente,
                   String apellidosCliente,
                   String dniNifCliente,
                   String emailCliente,
                   int telefonoCliente,
                   LocalDateTime fechaCreacionCliente) {
        this.idCliente = ++contadorIds;
        this.nombreCliente = nombreCliente;
        this.apellidosCliente = apellidosCliente;
        this.dniNifCliente = dniNifCliente;
        this.emailCliente = emailCliente;
        this.telefonoCliente = telefonoCliente;
        this.fechaCreacionCliente = fechaCreacionCliente;
    }
}