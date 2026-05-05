CREATE TABLE IF NOT EXISTS movimientos (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    numero_cuenta VARCHAR(34) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    cantidad NUMERIC(15, 2) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_id_fecha
    ON movimientos (cuenta_id, fecha DESC);
