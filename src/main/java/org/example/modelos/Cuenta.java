package org.example.modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Cuenta {
    private long idCuenta;
    private Cliente dueñoCuenta;
    private String numeroCuenta;
    private BigDecimal saldoCuenta;
    private List<Movimiento> listaMovimientosCuenta;
    private LocalDateTime fechaCreacionCuenta;

}
