# Documentacion Del Modulo 4

Esta carpeta contiene material de apoyo para ejecutar, probar y revisar NovaBank Digital Services en su version de microservicios sincronicos.

## Contenido

```text
docs/
|-- README.md
|-- sql/
|   |-- README.md
|   |-- 01-novabank-auth.sql
|   |-- 02-novabank-clientes.sql
|   |-- 03-novabank-cuentas.sql
|   |-- 04-novabank-operaciones.sql
|-- postman/
```

## SQL

Los scripts de `docs/sql` preparan las cuatro bases PostgreSQL usadas por los servicios:

- `novabank_auth`
- `novabank_clientes`
- `novabank_cuentas`
- `novabank_operaciones`

La guia de ejecucion esta en [docs/sql/README.md](sql/README.md).

## Postman

La carpeta `docs/postman` contiene la coleccion de apoyo para validar el flujo funcional mediante el Gateway.

El valor recomendado para la variable de entorno o coleccion es:

```text
baseUrl = http://localhost:8080
```

El flujo principal debe pasar por `api-gateway`, no por los puertos internos de los servicios.

## Swagger

Swagger se revisa por microservicio:

- `auth-server`: `http://localhost:9000/swagger-ui/index.html`
- `cliente-service`: `http://localhost:8081/swagger-ui/index.html`
- `cuenta-service`: `http://localhost:8082/swagger-ui/index.html`
- `operacion-service`: `http://localhost:8083/swagger-ui/index.html`

El Gateway no agrega Swagger en esta entrega.
