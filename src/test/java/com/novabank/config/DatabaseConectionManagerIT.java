package com.novabank.config;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DatabaseConectionManagerIT {

    @Test
    void getConnection_debeAbrirConexionReal() throws Exception {
        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
        }
    }
}
