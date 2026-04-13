package com.novabank.persistence.jdbc;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para CuentaRepositoryJdbc.
 *
 * Requiere PostgreSQL disponible y variables de entorno NOVABANK_DB_* configuradas.
 */
class CuentaRepositoryJdbcIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;
    private CuentaRepositoryJdbc cuentaRepositoryJdbc;

    @BeforeEach
    void setUp() {
        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
        cuentaRepositoryJdbc = new CuentaRepositoryJdbc();
    }

    @Test
    void guardarCuenta_yBuscarNumeroCuenta_debePersistirYRecuperarCuenta() {
        Cliente cliente = crearClientePersistido();
        String numeroCuenta = generarNumeroCuentaUnico();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(numeroCuenta)
                .saldoCuenta(BigDecimal.valueOf(150.75))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta);

        Cuenta recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(numeroCuenta);

        assertNotNull(recuperada);
        assertEquals(numeroCuenta, recuperada.getNumeroCuenta());
        assertEquals(BigDecimal.valueOf(150.75), recuperada.getSaldoCuenta());
        assertNotNull(recuperada.getDueñoCuenta());
        assertEquals(cliente.getIdCliente(), recuperada.getDueñoCuenta().getIdCliente());
    }

    @Test
    void listarCuentasCliente_debeRetornarLasCuentasDelTitular() {
        Cliente cliente = crearClientePersistido();

        Cuenta cuentaUno = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(10))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        Cuenta cuentaDos = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(20))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuentaUno);
        cuentaRepositoryJdbc.guardarCuenta(cuentaDos);

        List<Cuenta> cuentas = cuentaRepositoryJdbc.listarCuentasCliente(cliente.getIdCliente());

        assertNotNull(cuentas);
        assertEquals(2, cuentas.size());
    }

    private Cliente crearClientePersistido() {
        String sufijo = String.valueOf(System.currentTimeMillis());
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
        long sufijo = Math.abs(System.nanoTime() % 1_000_000_000_000_000_000L);
        return "ES91" + String.format("%018d", sufijo);
    }
}