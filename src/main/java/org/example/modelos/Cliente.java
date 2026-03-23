package org.example.modelos;

import java.util.List;

public class Cliente {
    private static long contadorIds = 1000L;

    private long idCliente;
    private String nombreCliente;
    private String apellidosCliente;
    private String dniNifCliente;
    private String emailCliente;
    private int telefonoCliente;
    private List<Cuenta> listaCuentasCliente;
    private String fechaCreacionCliente;
}
