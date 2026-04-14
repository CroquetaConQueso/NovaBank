package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.persistence.jdbc.ClienteRepositoryJdbc;
import com.novabank.persistence.jdbc.CuentaRepositoryJdbc;
import com.novabank.persistence.jdbc.MovimientoRepositoryJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovimientoServicioRollbackIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;
    private CuentaRepositoryJdbc cuentaRepositoryJdbc;
    private MovimientoRepositoryJdbc movimientoRepositoryJdbc;

    @BeforeEach
    void setUp() {
        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
        cuentaRepositoryJdbc = new CuentaRepositoryJdbc();
        movimientoRepositoryJdbc = new MovimientoRepositoryJdbc();
    }

    @Test
    void transferir_siFallaElSegundoMovimiento_debeHacerRollbackReal() {
        Cuenta origen = crearCuentaPersistida(BigDecimal.valueOf(100));
        Cuenta destino = crearCuentaPersistida(BigDecimal.valueOf(50));

        MovimientoRepositoryJdbc movimientoSpy = new MovimientoRepositoryJdbc() {
            private int llamadas = 0;

            @Override
            public void guardarMovimiento(Connection connection, Movimiento nuevoMovimiento) {
                llamadas++;
                if (llamadas == 2) {
                    throw new RuntimeException("Fallo provocado en el segundo movimiento");
                }
                super.guardarMovimiento(connection, nuevoMovimiento);
            }
        };

        MovimientoServicio movimientoServicio =
                new MovimientoServicio(cuentaRepositoryJdbc, movimientoSpy);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> movimientoServicio.transferir(
                        origen.getNumeroCuenta(),
                        destino.getNumeroCuenta(),
                        BigDecimal.valueOf(30)
                )
        );

        assertEquals("Fallo provocado en el segundo movimiento", ex.getMessage());

        Cuenta origenRecuperada =
                cuentaRepositoryJdbc.buscarNumeroCuenta(origen.getNumeroCuenta()).orElseThrow();
        Cuenta destinoRecuperada =
                cuentaRepositoryJdbc.buscarNumeroCuenta(destino.getNumeroCuenta()).orElseThrow();

        assertEquals(0, BigDecimal.valueOf(100).compareTo(origenRecuperada.getSaldoCuenta()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(destinoRecuperada.getSaldoCuenta()));

        List<Movimiento> movimientosOrigen =
                movimientoRepositoryJdbc.obtenerMovimientosCuenta(origen.getNumeroCuenta());
        List<Movimiento> movimientosDestino =
                movimientoRepositoryJdbc.obtenerMovimientosCuenta(destino.getNumeroCuenta());

        assertTrue(movimientosOrigen.isEmpty());
        assertTrue(movimientosDestino.isEmpty());
    }

    @Test
    void transferir_siFallaLaSegundaActualizacionDeSaldo_debeHacerRollbackReal() {
        Cuenta origen = crearCuentaPersistida(BigDecimal.valueOf(100));
        Cuenta destino = crearCuentaPersistida(BigDecimal.valueOf(50));

        CuentaRepositoryJdbc cuentaSpy = new CuentaRepositoryJdbc() {
            private int llamadas = 0;

            @Override
            public void actualizarSaldo(Connection connection, String numeroCuenta, BigDecimal nuevoSaldo) {
                llamadas++;
                if (llamadas == 2) {
                    throw new RuntimeException("Fallo provocado en la segunda actualización");
                }
                super.actualizarSaldo(connection, numeroCuenta, nuevoSaldo);
            }
        };

        MovimientoServicio movimientoServicio =
                new MovimientoServicio(cuentaSpy, movimientoRepositoryJdbc);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> movimientoServicio.transferir(
                        origen.getNumeroCuenta(),
                        destino.getNumeroCuenta(),
                        BigDecimal.valueOf(30)
                )
        );

        assertEquals("Fallo provocado en la segunda actualización", ex.getMessage());

        Cuenta origenRecuperada =
                cuentaRepositoryJdbc.buscarNumeroCuenta(origen.getNumeroCuenta()).orElseThrow();
        Cuenta destinoRecuperada =
                cuentaRepositoryJdbc.buscarNumeroCuenta(destino.getNumeroCuenta()).orElseThrow();

        assertEquals(0, BigDecimal.valueOf(100).compareTo(origenRecuperada.getSaldoCuenta()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(destinoRecuperada.getSaldoCuenta()));

        List<Movimiento> movimientosOrigen =
                movimientoRepositoryJdbc.obtenerMovimientosCuenta(origen.getNumeroCuenta());
        List<Movimiento> movimientosDestino =
                movimientoRepositoryJdbc.obtenerMovimientosCuenta(destino.getNumeroCuenta());

        assertTrue(movimientosOrigen.isEmpty());
        assertTrue(movimientosDestino.isEmpty());
    }

    private Cuenta crearCuentaPersistida(BigDecimal saldoInicial) {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(saldoInicial)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta);
        return cuenta;
    }

    private Cliente crearClientePersistido() {
        String sufijo = String.valueOf(System.nanoTime());
        String ochoDigitos = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
        int telefono = Integer.parseInt("6" + ochoDigitos.substring(1));

        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente(ochoDigitos + "Z")
                .emailCliente("carlos" + sufijo + "@example.com")
                .telefonoCliente(telefono)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        clienteRepositoryJdbc.anadirCliente(cliente);
        return cliente;
    }

    private String generarNumeroCuentaUnico() {
        long sufijo = Math.abs(System.nanoTime() % 1_000_000_000_000L);
        return "ES91210000" + String.format("%012d", sufijo);
    }
}