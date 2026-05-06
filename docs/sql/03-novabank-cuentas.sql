\connect postgres

-- Crea la base de datos de cuentas si todavia no existe.
SELECT 'CREATE DATABASE novabank_cuentas'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_cuentas'
)\gexec

\connect novabank_cuentas

-- Secuencia funcional usada por cuenta-service para generar numeros de cuenta.
CREATE TABLE IF NOT EXISTS account_number_sequence (
    id BIGINT PRIMARY KEY,
    next_value BIGINT NOT NULL
);

INSERT INTO account_number_sequence (id, next_value)
VALUES (1, 1)
ON CONFLICT (id) DO NOTHING;

-- cliente_id es una referencia logica a cliente-service, no una FK entre bases.
CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(34) NOT NULL,
    cliente_id BIGINT NOT NULL,
    saldo NUMERIC(15, 2) NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_cuentas_numero_cuenta UNIQUE (numero_cuenta)
);

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id
    ON cuentas (cliente_id);
