\connect postgres

-- Crea la base de datos de clientes si todavia no existe.
SELECT 'CREATE DATABASE novabank_clientes'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'novabank_clientes'
)\gexec

\connect novabank_clientes

-- cliente-service es duenio exclusivo de los datos de clientes.
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
