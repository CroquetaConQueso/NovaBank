\connect postgres

-- Crea la base de datos de operaciones si todavia no existe.
SELECT 'CREATE DATABASE novabank_operaciones'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_operaciones'
)\gexec

\connect novabank_operaciones

-- operacion-service registra el historial financiero sin FK real hacia cuentas.
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

-- Idempotencia publica mediante header Idempotency-Key.
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
