\connect postgres

SELECT 'CREATE DATABASE novabank_auth'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_auth'
)\gexec

\connect novabank_auth

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL
);

\connect postgres

SELECT 'CREATE DATABASE novabank_clientes'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_clientes'
)\gexec

\connect novabank_clientes

CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    dni VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_clientes_dni UNIQUE (dni),
    CONSTRAINT uk_clientes_email UNIQUE (email),
    CONSTRAINT uk_clientes_telefono UNIQUE (telefono)
);

\connect postgres

SELECT 'CREATE DATABASE novabank_cuentas'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_cuentas'
)\gexec

\connect novabank_cuentas

CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(34) NOT NULL,
    cliente_id BIGINT NOT NULL,
    saldo NUMERIC(15,2) NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT,
    CONSTRAINT uk_cuentas_numero_cuenta UNIQUE (numero_cuenta)
);

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id
    ON cuentas (cliente_id);

CREATE TABLE IF NOT EXISTS account_number_sequence (
    id BIGINT PRIMARY KEY,
    next_value BIGINT NOT NULL
);

INSERT INTO account_number_sequence (id, next_value)
VALUES (1, 1)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS operaciones_idempotentes (
    id BIGSERIAL PRIMARY KEY,
    operation_id VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_operaciones_idempotentes_operation_id UNIQUE (operation_id),
    CONSTRAINT chk_operaciones_idempotentes_estado
        CHECK (estado IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_operaciones_idempotentes_estado
    ON operaciones_idempotentes (estado);

\connect postgres

SELECT 'CREATE DATABASE novabank_operaciones'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_operaciones'
)\gexec

\connect novabank_operaciones

CREATE TABLE IF NOT EXISTS movimientos (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    numero_cuenta VARCHAR(34) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    cantidad NUMERIC(15,2) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movimientos_tipo
        CHECK (tipo IN ('DEPOSITO', 'RETIRO', 'TRANSFERENCIA_SALIENTE', 'TRANSFERENCIA_ENTRANTE'))
);

CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_id
    ON movimientos (cuenta_id);

CREATE INDEX IF NOT EXISTS idx_movimientos_fecha
    ON movimientos (fecha);

CREATE TABLE IF NOT EXISTS operaciones_publicas_idempotentes (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(150) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    tipo_operacion VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    response_json TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_operaciones_publicas_idempotentes_key UNIQUE (idempotency_key),
    CONSTRAINT chk_operaciones_publicas_idempotentes_estado
        CHECK (estado IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_operaciones_publicas_idempotentes_estado
    ON operaciones_publicas_idempotentes (estado);
