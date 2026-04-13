package com.novabank.persistence.jdbc;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.exception.NovaBankException;
import com.novabank.persistence.repository.CuentaRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC del contrato de persistencia para cuentas.
 *
 * Traduce operaciones del repositorio a sentencias SQL contra PostgreSQL,
 * sin incorporar lógica de negocio.
 */
public class CuentaRepositoryJdbc implements CuentaRepository {

    @Override
    public void guardarCuenta(Cuenta nuevaCuenta) {
        String sql = """
                INSERT INTO cuentas (numero_cuenta, cliente_id, saldo, fecha_creacion)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (numero_cuenta) DO UPDATE SET
                    cliente_id = EXCLUDED.cliente_id,
                    saldo = EXCLUDED.saldo
                """;

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nuevaCuenta.getNumeroCuenta());
            statement.setLong(2, nuevaCuenta.getDueñoCuenta().getIdCliente());
            statement.setBigDecimal(3, nuevaCuenta.getSaldoCuenta());
            statement.setTimestamp(4, Timestamp.valueOf(nuevaCuenta.getFechaCreacionCuenta()));

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new NovaBankException("No se pudo guardar la cuenta en la base de datos.");
            }

        } catch (SQLException ex) {
            throw new NovaBankException("Error al guardar la cuenta en la base de datos.", ex);
        }
    }

    @Override
    public Cuenta buscarNumeroCuenta(String numeroCuenta) {
        String sql = """
                SELECT
                    c.id AS cuenta_id,
                    c.numero_cuenta,
                    c.saldo,
                    c.fecha_creacion AS cuenta_fecha_creacion,
                    cl.id AS cliente_id,
                    cl.nombre,
                    cl.apellidos,
                    cl.dni,
                    cl.email,
                    cl.telefono,
                    cl.fecha_creacion AS cliente_fecha_creacion
                FROM cuentas c
                JOIN clientes cl ON cl.id = c.cliente_id
                WHERE c.numero_cuenta = ?
                """;

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, numeroCuenta);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearCuenta(resultSet);
                }
                return null;
            }

        } catch (SQLException ex) {
            throw new NovaBankException("Error al buscar la cuenta por número.", ex);
        }
    }

    @Override
    public List<Cuenta> listarCuentasCliente(Long idBuscar) {
        String sql = """
                SELECT
                    c.id AS cuenta_id,
                    c.numero_cuenta,
                    c.saldo,
                    c.fecha_creacion AS cuenta_fecha_creacion,
                    cl.id AS cliente_id,
                    cl.nombre,
                    cl.apellidos,
                    cl.dni,
                    cl.email,
                    cl.telefono,
                    cl.fecha_creacion AS cliente_fecha_creacion
                FROM cuentas c
                JOIN clientes cl ON cl.id = c.cliente_id
                WHERE c.cliente_id = ?
                ORDER BY c.id
                """;

        List<Cuenta> cuentas = new ArrayList<>();

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, idBuscar);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cuentas.add(mapearCuenta(resultSet));
                }
            }

            return cuentas;

        } catch (SQLException ex) {
            throw new NovaBankException("Error al listar cuentas del cliente.", ex);
        }
    }

    private Cuenta mapearCuenta(ResultSet resultSet) throws SQLException {
        Cliente cliente = mapearCliente(resultSet);

        Timestamp fechaCuenta = resultSet.getTimestamp("cuenta_fecha_creacion");
        LocalDateTime fechaCreacionCuenta = fechaCuenta != null ? fechaCuenta.toLocalDateTime() : null;

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(resultSet.getString("numero_cuenta"))
                .saldoCuenta(resultSet.getBigDecimal("saldo"))
                .fechaCreacionCuenta(fechaCreacionCuenta)
                .build();

        cuenta.setIdCuenta(resultSet.getLong("cuenta_id"));
        return cuenta;
    }

    private Cliente mapearCliente(ResultSet resultSet) throws SQLException {
        Timestamp fechaCliente = resultSet.getTimestamp("cliente_fecha_creacion");
        LocalDateTime fechaCreacionCliente = fechaCliente != null ? fechaCliente.toLocalDateTime() : null;

        Cliente cliente = Cliente.builder()
                .nombreCliente(resultSet.getString("nombre"))
                .apellidosCliente(resultSet.getString("apellidos"))
                .dniNifCliente(resultSet.getString("dni"))
                .emailCliente(resultSet.getString("email"))
                .telefonoCliente(Integer.parseInt(resultSet.getString("telefono")))
                .fechaCreacionCliente(fechaCreacionCliente)
                .build();

        cliente.setIdCliente(resultSet.getLong("cliente_id"));
        return cliente;
    }
}