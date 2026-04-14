package com.novabank.persistence.jdbc;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.NovaBankException;
import com.novabank.persistence.repository.MovimientoRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC del contrato de persistencia para movimientos.
 */
public class MovimientoRepositoryJdbc implements MovimientoRepository {

    @Override
    public void guardarMovimiento(Movimiento nuevoMovimiento) {
        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            guardarMovimiento(connection, nuevoMovimiento);
        } catch (SQLException ex) {
            throw new NovaBankException("Error al guardar el movimiento en la base de datos.", ex);
        }
    }

    @Override
    public void guardarMovimiento(Connection connection, Movimiento nuevoMovimiento) {
        String sql = """
            INSERT INTO movimientos (cuenta_id, tipo, cantidad, fecha)
            VALUES (
                (SELECT id FROM cuentas WHERE numero_cuenta = ?),
                ?, ?, ?
            )
            RETURNING id
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nuevoMovimiento.getCuentaAsignada().getNumeroCuenta());
            statement.setString(2, nuevoMovimiento.getTipoMov().name());
            statement.setBigDecimal(3, nuevoMovimiento.getCantidadMovimiento());
            statement.setTimestamp(4, Timestamp.valueOf(nuevoMovimiento.getFechaCreacionMov()));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    nuevoMovimiento.setIdMovimiento(resultSet.getLong("id"));
                } else {
                    throw new NovaBankException("No se pudo recuperar el id del movimiento guardado.");
                }
            }
        } catch (SQLException ex) {
            throw new NovaBankException("Error al guardar el movimiento en la base de datos.", ex);
        }
    }

    @Override
    public List<Movimiento> obtenerMovimientosCuenta(String numeroCuentaBuscar) {
        String sql = """
                SELECT
                    m.id AS movimiento_id,
                    m.tipo,
                    m.cantidad,
                    m.fecha AS movimiento_fecha,
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
                FROM movimientos m
                JOIN cuentas c ON c.id = m.cuenta_id
                JOIN clientes cl ON cl.id = c.cliente_id
                WHERE c.numero_cuenta = ?
                ORDER BY m.fecha DESC, m.id DESC
                """;

        List<Movimiento> movimientos = new ArrayList<>();

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, numeroCuentaBuscar);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    movimientos.add(mapearMovimiento(resultSet));
                }
            }

            return movimientos;

        } catch (SQLException ex) {
            throw new NovaBankException("Error al obtener movimientos de la cuenta.", ex);
        }
    }

    @Override
    public List<Movimiento> obtenerMovimientosFecha(String numeroCuentaBuscar, LocalDate inicio, LocalDate fin) {
        String sql = """
                SELECT
                    m.id AS movimiento_id,
                    m.tipo,
                    m.cantidad,
                    m.fecha AS movimiento_fecha,
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
                FROM movimientos m
                JOIN cuentas c ON c.id = m.cuenta_id
                JOIN clientes cl ON cl.id = c.cliente_id
                WHERE c.numero_cuenta = ?
                  AND m.fecha >= ?
                  AND m.fecha < ?
                ORDER BY m.fecha DESC, m.id DESC
                """;

        List<Movimiento> movimientos = new ArrayList<>();

        LocalDateTime inicioDia = inicio.atStartOfDay();
        LocalDateTime finExclusivo = fin.plusDays(1).atStartOfDay();

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, numeroCuentaBuscar);
            statement.setTimestamp(2, Timestamp.valueOf(inicioDia));
            statement.setTimestamp(3, Timestamp.valueOf(finExclusivo));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    movimientos.add(mapearMovimiento(resultSet));
                }
            }

            return movimientos;

        } catch (SQLException ex) {
            throw new NovaBankException("Error al obtener movimientos por rango de fechas.", ex);
        }
    }

    @Override
    public boolean soportaTransacciones() {
        return true;
    }

    private Movimiento mapearMovimiento(ResultSet resultSet) throws SQLException {
        Cuenta cuenta = mapearCuenta(resultSet);

        Timestamp fechaMovimiento = resultSet.getTimestamp("movimiento_fecha");
        LocalDateTime fechaCreacionMovimiento =
                fechaMovimiento != null ? fechaMovimiento.toLocalDateTime() : null;

        Movimiento movimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.valueOf(resultSet.getString("tipo")))
                .cantidadMovimiento(resultSet.getBigDecimal("cantidad"))
                .fechaCreacionMov(fechaCreacionMovimiento)
                .build();

        movimiento.setIdMovimiento(resultSet.getLong("movimiento_id"));
        return movimiento;
    }

    private Cuenta mapearCuenta(ResultSet resultSet) throws SQLException {
        Cliente cliente = mapearCliente(resultSet);

        Timestamp fechaCuenta = resultSet.getTimestamp("cuenta_fecha_creacion");
        LocalDateTime fechaCreacionCuenta =
                fechaCuenta != null ? fechaCuenta.toLocalDateTime() : null;

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
        LocalDateTime fechaCreacionCliente =
                fechaCliente != null ? fechaCliente.toLocalDateTime() : null;

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