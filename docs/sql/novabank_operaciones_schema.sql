CREATE TABLE IF NOT EXISTS movimientos (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    numero_cuenta VARCHAR(34) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    cantidad NUMERIC(15, 2) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movimientos_tipo CHECK (
        tipo IN (
            'DEPOSITO',
            'RETIRO',
            'TRANSFERENCIA_SALIENTE',
            'TRANSFERENCIA_ENTRANTE'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_id_fecha
    ON movimientos (cuenta_id, fecha DESC);

CREATE INDEX IF NOT EXISTS idx_movimientos_fecha
    ON movimientos (fecha);

CREATE INDEX IF NOT EXISTS idx_movimientos_tipo
    ON movimientos (tipo);
