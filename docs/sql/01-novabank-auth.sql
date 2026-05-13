\connect postgres

-- Crea la base de datos de autenticacion si todavia no existe.
SELECT 'CREATE DATABASE novabank_auth'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_auth'
)\gexec

\connect novabank_auth

-- auth-server persiste usuarios y credenciales cifradas.
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL
);
