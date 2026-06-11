# Documentacion Interna - NovaBank Modulo 5

Esta carpeta contiene material de apoyo para ejecutar, probar y revisar NovaBank en su estado final del Modulo 5.

## Contenido

```text
docs/
|-- README.md
|-- sql/
|   |-- README.md
|   |-- 00-novabank-modulo5-completo.sql
|   |-- 01-novabank-auth.sql
|   |-- 02-novabank-clientes.sql
|   |-- 03-novabank-cuentas.sql
|   |-- 04-novabank-operaciones.sql
|-- postman/
```

## SQL

La carpeta `docs/sql` contiene los scripts para preparar las bases PostgreSQL usadas por los servicios reactivos:

- `novabank_auth`
- `novabank_clientes`
- `novabank_cuentas`
- `novabank_operaciones`

El script recomendado para preparar todo el entorno local es:

```powershell
psql -U postgres -f docs/sql/00-novabank-modulo5-completo.sql
```

La guia completa esta en [docs/sql/README.md](sql/README.md).

## Postman

La carpeta `docs/postman` conserva la coleccion disponible en el repositorio para pruebas manuales. El flujo recomendado debe pasar por el Gateway:

```text
baseUrl = http://localhost:8080
```

## Swagger

Swagger documenta APIs HTTP REST. Kafka, SAGA interna, eventos, topics, consumer groups y state stores se documentan en README/PDF, no como rutas falsas.

OpenAPI agregado desde el Gateway:

- `auth-server`: `http://localhost:8080/v3/api-docs/auth-server`
- `cliente-service`: `http://localhost:8080/v3/api-docs/cliente-service`
- `cuenta-service`: `http://localhost:8080/v3/api-docs/cuenta-service`
- `operacion-service`: `http://localhost:8080/v3/api-docs/operacion-service`
- `exchange-rate-mock-service`: `http://localhost:8080/v3/api-docs/exchange-rate-mock-service`

El Gateway agrega los JSON OpenAPI. La interfaz Swagger UI se revisa directamente en cada microservicio.

Swagger directo por microservicio:

- `auth-server`: `http://localhost:9000/swagger-ui/index.html`
- `cliente-service`: `http://localhost:8081/swagger-ui/index.html`
- `cuenta-service`: `http://localhost:8082/swagger-ui/index.html`
- `operacion-service`: `http://localhost:8083/swagger-ui/index.html`
- `exchange-rate-mock-service`: `http://localhost:8084/swagger-ui/index.html`

`notificacion-service` no expone API REST funcional de negocio.
