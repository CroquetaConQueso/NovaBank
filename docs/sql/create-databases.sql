SELECT 'CREATE DATABASE novabank_clientes'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'novabank_clientes')\gexec

SELECT 'CREATE DATABASE novabank_cuentas'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'novabank_cuentas')\gexec

SELECT 'CREATE DATABASE novabank_operaciones'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'novabank_operaciones')\gexec

SELECT 'CREATE DATABASE novabank_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'novabank_auth')\gexec
