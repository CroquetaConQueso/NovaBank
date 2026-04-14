package com.novabank.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa un cliente del sistema bancario.
 *
 * En persistencia JDBC, el identificador lo genera PostgreSQL.
 * La entidad no debe autogenerar IDs por su cuenta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Cliente {

    private long idCliente;
    private String nombreCliente;
    private String apellidosCliente;
    private String dniNifCliente;
    private String emailCliente;
    private int telefonoCliente;
    private LocalDateTime fechaCreacionCliente;
}