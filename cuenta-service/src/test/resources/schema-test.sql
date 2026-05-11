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

MERGE INTO account_number_sequence KEY(id)
VALUES (1, 1);
