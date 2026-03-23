package org.example.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class Cuenta {
    private long idCuenta;
    private Cliente dueñoCuenta;
    private String numeroCuenta;
    private BigDecimal saldoCuenta;
    private List<Movimiento> listaMovimientosCuenta;
    private LocalDateTime fechaCreacionCuenta;

}
