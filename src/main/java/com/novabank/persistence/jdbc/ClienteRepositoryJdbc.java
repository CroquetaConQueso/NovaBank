package com.novabank.persistence.jdbc;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import com.novabank.exception.NovaBankException;
import com.novabank.persistence.repository.ClienteRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC del contrato de persistencia para clientes.
 *
 * Su responsabilidad es traducir operaciones del repositorio a sentencias SQL
 * contra PostgreSQL, sin incorporar lógica de negocio.
 */
public class ClienteRepositoryJdbc implements ClienteRepository {

    @Override
    public void anadirCliente(Cliente nuevoCliente) {
        String sql = """
                INSERT INTO clientes (nombre, apellidos, dni, email, telefono, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, nuevoCliente.getNombreCliente());
            statement.setString(2, nuevoCliente.getApellidosCliente());
            statement.setString(3, nuevoCliente.getDniNifCliente());
            statement.setString(4, nuevoCliente.getEmailCliente());
            statement.setString(5, String.valueOf(nuevoCliente.getTelefonoCliente()));
            statement.setTimestamp(6, Timestamp.valueOf(nuevoCliente.getFechaCreacionCliente()));

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new NovaBankException("No se pudo insertar el cliente en la base de datos.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    nuevoCliente.setIdCliente(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException ex) {
            throw new NovaBankException("Error al insertar cliente en la base de datos.", ex);
        }
    }

    @Override
    public Cliente buscarIdCliente(Long idBusqueda) {
        String sql = """
                SELECT id, nombre, apellidos, dni, email, telefono, fecha_creacion
                FROM clientes
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, idBusqueda);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearCliente(resultSet);
                }
                return null;
            }

        } catch (SQLException ex) {
            throw new NovaBankException("Error al buscar cliente por ID.", ex);
        }
    }

    @Override
    public Cliente buscarDniCliente(String dniNif) {
        String sql = """
                SELECT id, nombre, apellidos, dni, email, telefono, fecha_creacion
                FROM clientes
                WHERE UPPER(dni) = UPPER(?)
                """;

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, dniNif);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearCliente(resultSet);
                }
                return null;
            }

        } catch (SQLException ex) {
            throw new NovaBankException("Error al buscar cliente por DNI.", ex);
        }
    }

    @Override
    public Cliente buscarEmailCliente(String email) {
        String sql = """
                SELECT id, nombre, apellidos, dni, email, telefono, fecha_creacion
                FROM clientes
                WHERE LOWER(email) = LOWER(?)
                """;

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearCliente(resultSet);
                }
                return null;
            }

        } catch (SQLException ex) {
            throw new NovaBankException("Error al buscar cliente por email.", ex);
        }
    }

    @Override
    public Cliente buscarTelefonoCliente(int telefonoCli) {
        String sql = """
                SELECT id, nombre, apellidos, dni, email, telefono, fecha_creacion
                FROM clientes
                WHERE telefono = ?
                """;

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, String.valueOf(telefonoCli));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearCliente(resultSet);
                }
                return null;
            }

        } catch (SQLException ex) {
            throw new NovaBankException("Error al buscar cliente por teléfono.", ex);
        }
    }

    @Override
    public List<Cliente> obtenerClientes() {
        String sql = """
                SELECT id, nombre, apellidos, dni, email, telefono, fecha_creacion
                FROM clientes
                ORDER BY id
                """;

        List<Cliente> clientes = new ArrayList<>();

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                clientes.add(mapearCliente(resultSet));
            }

            return clientes;

        } catch (SQLException ex) {
            throw new NovaBankException("Error al obtener la lista de clientes.", ex);
        }
    }

    private Cliente mapearCliente(ResultSet resultSet) throws SQLException {
        Timestamp fechaCreacion = resultSet.getTimestamp("fecha_creacion");
        LocalDateTime fechaCreacionCliente = fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null;

        Cliente cliente = Cliente.builder()
                .nombreCliente(resultSet.getString("nombre"))
                .apellidosCliente(resultSet.getString("apellidos"))
                .dniNifCliente(resultSet.getString("dni"))
                .emailCliente(resultSet.getString("email"))
                .telefonoCliente(Integer.parseInt(resultSet.getString("telefono")))
                .fechaCreacionCliente(fechaCreacionCliente)
                .build();

        cliente.setIdCliente(resultSet.getLong("id"));
        return cliente;
    }
}