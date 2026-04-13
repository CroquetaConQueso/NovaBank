package com.novabank.config;

import com.novabank.persistence.jdbc.ClienteRepositoryJdbc;
import com.novabank.persistence.jdbc.CuentaRepositoryJdbc;
import com.novabank.persistence.jdbc.MovimientoRepositoryJdbc;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test básico de la factoría de repositorios.
 *
 * Verifica que la aplicación queda cableada a implementaciones JDBC.
 */
class RepositoryFactoryTest {

    @Test
    void crearClienteRepository_debeRetornarImplementacionJdbc() {
        ClienteRepository repository = RepositoryFactory.crearClienteRepository();

        assertNotNull(repository);
        assertInstanceOf(ClienteRepositoryJdbc.class, repository);
    }

    @Test
    void crearCuentaRepository_debeRetornarImplementacionJdbc() {
        CuentaRepository repository = RepositoryFactory.crearCuentaRepository();

        assertNotNull(repository);
        assertInstanceOf(CuentaRepositoryJdbc.class, repository);
    }

    @Test
    void crearMovimientoRepository_debeRetornarImplementacionJdbc() {
        MovimientoRepository repository = RepositoryFactory.crearMovimientoRepository();

        assertNotNull(repository);
        assertInstanceOf(MovimientoRepositoryJdbc.class, repository);
    }
}