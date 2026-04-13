package com.novabank.config;

import com.novabank.persistence.jdbc.ClienteRepositoryJdbc;
import com.novabank.persistence.jdbc.CuentaRepositoryJdbc;
import com.novabank.persistence.jdbc.MovimientoRepositoryJdbc;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;

/**
 * Factoría simple para centralizar qué implementación de repositorio usa la aplicación.
 *
 * En esta fase del módulo 2, la aplicación de consola pasa a trabajar con JDBC
 * como implementación principal.
 */
public final class RepositoryFactory {

    private RepositoryFactory() {
    }

    public static ClienteRepository crearClienteRepository() {
        return new ClienteRepositoryJdbc();
    }

    public static CuentaRepository crearCuentaRepository() {
        return new CuentaRepositoryJdbc();
    }

    public static MovimientoRepository crearMovimientoRepository() {
        return new MovimientoRepositoryJdbc();
    }
}