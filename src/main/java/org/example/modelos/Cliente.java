package org.example.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Cliente {
    private static long contadorIds = 1000L;

    private long idCliente = ++contadorIds;
    private String nombreCliente;
    private String apellidosCliente;
    private String dniNifCliente;
    private String emailCliente;
    private int telefonoCliente;
    private List<Cuenta> listaCuentasCliente;
    private String fechaCreacionCliente;
}
