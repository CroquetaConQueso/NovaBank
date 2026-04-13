CREATE DATABASE NovaBank;
\c NovaBank

CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE CHECK (
        POSITION('@' IN email) > 1
        AND POSITION('.' IN email) > 3
    ),
    telefono VARCHAR(20) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(34) NOT NULL UNIQUE,
    cliente_id BIGINT NOT NULL,
    saldo NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (saldo >= 0),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cuentas_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES clientes(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS movimientos (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    tipo VARCHAR(50) NOT NULL CHECK (
        tipo IN (
            'DEPOSITO',
            'RETIRO',
            'TRANSFERENCIA_SALIENTE',
            'TRANSFERENCIA_ENTRANTE'
        )
    ),
    cantidad NUMERIC(15,2) NOT NULL CHECK (cantidad > 0),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimientos_cuenta
        FOREIGN KEY (cuenta_id)
        REFERENCES cuentas(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id
    ON cuentas(cliente_id);

CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_id
    ON movimientos(cuenta_id);

CREATE INDEX IF NOT EXISTS idx_movimientos_fecha
    ON movimientos(fecha);