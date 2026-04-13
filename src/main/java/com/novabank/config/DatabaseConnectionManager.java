package com.novabank.config;

import com.novabank.exception.NovaBankException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la creación de conexiones JDBC a PostgreSQL.
 *
 * Centraliza la lectura de configuración y evita repartir la lógica de
 * conexión por distintas clases del proyecto.
 */
public final class DatabaseConnectionManager {

    private static DatabaseConnectionManager instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnectionManager() {
        this.url = leerObligatoria("NOVABANK_DB_URL");
        this.user = leerObligatoria("NOVABANK_DB_USER");
        this.password = leerObligatoria("NOVABANK_DB_PASSWORD");
    }

    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    public static Connection getConnection() {
        DatabaseConnectionManager manager = getInstance();

        try {
            return DriverManager.getConnection(manager.url, manager.user, manager.password);
        } catch (SQLException ex) {
            throw new NovaBankException("No se ha podido abrir la conexión con la base de datos.", ex);
        }
    }

    private String leerObligatoria(String... nombres) {
        for (String nombre : nombres) {
            String valor = System.getenv(nombre);

            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }

        throw new NovaBankException(
                "Falta alguna variable de entorno obligatoria de base de datos. " +
                        "Se esperaba una de estas: " + String.join(", ", nombres)
        );
    }
}