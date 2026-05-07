# NovaBank Digital Services - Microservicios Spring Cloud

Documentación principal de la versión final del **Módulo 4**.

NovaBank Digital Services evoluciona desde el monolito Spring Boot del Módulo 3 hacia una arquitectura de microservicios síncronos con Spring Cloud. El monolito antiguo ya no forma parte del reactor Maven final.

## 1. Descripción Del Proyecto

NovaBank Digital Services simula una entidad bancaria básica con las siguientes capacidades:

- Registro y autenticación de usuarios.
- Gestión de clientes.
- Gestión de cuentas bancarias.
- Consulta de saldo.
- Operaciones financieras:
  - depósito;
  - retirada;
  - transferencia entre cuentas.
- Registro de movimientos.
- Consulta de historial de movimientos.
- Filtrado de movimientos por rango de fechas.
- Validación de seguridad mediante JWT.
- Enrutamiento mediante API Gateway.
- Descubrimiento de servicios mediante Eureka.
- Configuración centralizada mediante Config Server.
- Comunicación síncrona entre microservicios mediante OpenFeign.
- Resiliencia mediante Resilience4j.
- Documentación OpenAPI/Swagger por microservicio.
- Pruebas automatizadas por capas.

## 2. Arquitectura General

El sistema se organiza como un proyecto Maven multi-módulo:

```text
Cliente HTTP / Postman / Swagger
        |
        v
api-gateway
        |
        v
auth-server / cliente-service / cuenta-service / operacion-service
        |
        v
PostgreSQL
```

Los servicios se registran en Eureka y obtienen su configuración desde Config Server.

## 3. Módulos Del Proyecto

```text
NovaBank/
|-- pom.xml
|-- eureka-server/
|-- config-server/
|-- api-gateway/
|-- auth-server/
|-- cliente-service/
|-- cuenta-service/
|-- operacion-service/
|-- docs/
|   |-- sql/
```

| Módulo | Responsabilidad | Puerto |
| --- | --- | --- |
| `eureka-server` | Registro y descubrimiento de servicios. | `8761` |
| `config-server` | Configuración centralizada desde un repositorio Git local externo. | `8888` |
| `api-gateway` | Punto de entrada HTTP del sistema. | `8080` |
| `auth-server` | Registro, login y validación de tokens JWT. | `9000` |
| `cliente-service` | Gestión de clientes bancarios. | `8081` |
| `cuenta-service` | Gestión de cuentas, saldos, generación de número de cuenta, control de concurrencia con `@Version` y tabla `account_number_sequence`. | `8082` |
| `operacion-service` | Depósitos, retiradas, transferencias e historial de movimientos. | `8083` |

El antiguo monolito del Módulo 3 no forma parte del reactor Maven final.

## 4. Tecnologías Utilizadas

- Java 17
- Maven
- Spring Boot 3.3.6
- Spring Cloud 2023.0.4
- Spring Cloud Netflix Eureka
- Spring Cloud Config Server
- Spring Cloud Gateway
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- OpenFeign
- Resilience4j
- PostgreSQL
- H2 para tests
- WireMock para contratos HTTP
- JUnit 5
- Mockito
- MockMvc
- WebTestClient
- Swagger/OpenAPI con `springdoc-openapi`

## 5. Requisitos Previos

Para ejecutar el proyecto en local se necesita:

- Java 17.
- Maven.
- PostgreSQL.
- Git.
- Puertos disponibles:
  - `8761`
  - `8888`
  - `8080`
  - `9000`
  - `8081`
  - `8082`
  - `8083`

Comprobación recomendada:

```powershell
java -version
mvn -version
git --version
```

El proyecto está configurado para Java 17. Aunque pueda ejecutarse con un JDK superior, se recomienda mantener Java 17 para evitar diferencias innecesarias en compilación, plugins o procesadores de anotaciones.

## 6. Bases De Datos PostgreSQL

La arquitectura del Módulo 4 usa una base de datos separada por servicio de negocio.

Bases necesarias:

```text
novabank_auth
novabank_clientes
novabank_cuentas
novabank_operaciones
```

Los scripts de referencia se encuentran en:

```text
docs/sql/
```

Contenido esperado:

```text
docs/sql/
|-- README.md
|-- 01-novabank-auth.sql
|-- 02-novabank-clientes.sql
|-- 03-novabank-cuentas.sql
|-- 04-novabank-operaciones.sql
```

| Script | Uso |
| --- | --- |
| `01-novabank-auth.sql` | Crea `novabank_auth` y la tabla `usuarios`. |
| `02-novabank-clientes.sql` | Crea `novabank_clientes` y la tabla `clientes`. |
| `03-novabank-cuentas.sql` | Crea `novabank_cuentas`, `cuentas` y `account_number_sequence`. |
| `04-novabank-operaciones.sql` | Crea `novabank_operaciones` y la tabla `movimientos`. |

Ejecución recomendada:

```powershell
psql -U postgres -f docs/sql/01-novabank-auth.sql
psql -U postgres -f docs/sql/02-novabank-clientes.sql
psql -U postgres -f docs/sql/03-novabank-cuentas.sql
psql -U postgres -f docs/sql/04-novabank-operaciones.sql
```

La guía completa está en [docs/sql/README.md](docs/sql/README.md).

Los servicios usan:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Hibernate no crea ni modifica automáticamente las tablas. Las bases y tablas deben existir antes de arrancar los servicios. Si falta una tabla, el servicio falla al iniciar, que es el comportamiento esperado con `validate`.

## 7. Config Server

`config-server` está configurado para leer desde un repositorio Git local externo:

```text
file:///C:/Users/Usuario/Desktop/config-repo
```

Ese directorio debe existir, estar inicializado como repositorio Git, estar en la rama `main` y tener al menos un commit.

Estructura esperada:

```text
config-repo/
|-- application.yml
|-- api-gateway.yml
|-- auth-server.yml
|-- cliente-service.yml
|-- cuenta-service.yml
|-- operacion-service.yml
```

Ejemplo de inicialización:

```powershell
cd C:\Users\Usuario\Desktop\config-repo
git init
git add .
git commit -m "Configuracion inicial de microservicios NovaBank"
```

El repositorio `config-repo` no debe estar versionado dentro del monorepo principal. La configuración centralizada se mantiene fuera del proyecto para simular un repositorio de configuración independiente.

Con Eureka y Config Server arrancados:

```powershell
Invoke-RestMethod http://localhost:8888/cliente-service/default | ConvertTo-Json -Depth 10
```

También pueden comprobarse otros servicios:

- [http://localhost:8888/auth-server/default](http://localhost:8888/auth-server/default)
- [http://localhost:8888/cliente-service/default](http://localhost:8888/cliente-service/default)
- [http://localhost:8888/cuenta-service/default](http://localhost:8888/cuenta-service/default)
- [http://localhost:8888/operacion-service/default](http://localhost:8888/operacion-service/default)
- [http://localhost:8888/api-gateway/default](http://localhost:8888/api-gateway/default)

## 8. Orden De Arranque Local

Ejecutar cada servicio desde la raíz del proyecto en terminales separadas.

Orden recomendado:

1. `eureka-server`
2. `config-server`
3. `auth-server`
4. `cliente-service`
5. `cuenta-service`
6. `operacion-service`
7. `api-gateway`

Comandos:

```powershell
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl auth-server spring-boot:run
mvn -pl cliente-service spring-boot:run
mvn -pl cuenta-service spring-boot:run
mvn -pl operacion-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

La opción `-pl` indica a Maven que ejecute un módulo concreto del proyecto multi-módulo.

## 9. Comprobaciones Básicas

### Eureka

Con los servicios arrancados:

[http://localhost:8761](http://localhost:8761)

En Eureka deben aparecer, como mínimo:

- `CONFIG-SERVER`
- `AUTH-SERVER`
- `CLIENTE-SERVICE`
- `CUENTA-SERVICE`
- `OPERACION-SERVICE`
- `API-GATEWAY`

### Gateway

Punto de entrada funcional:

[http://localhost:8080](http://localhost:8080)

La colección Postman debe usar:

```text
baseUrl = http://localhost:8080
```

El flujo principal debe probarse a través del Gateway, no llamando directamente a los puertos internos de cada servicio.

## 10. Seguridad

La seguridad se centraliza en `api-gateway`.

Rutas públicas:

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/validate
```

Rutas protegidas:

```text
/api/clientes/**
/api/cuentas/**
/api/operaciones/**
```

Para acceder a rutas protegidas se debe enviar:

```http
Authorization: Bearer <token>
```

Flujo de autenticación:

1. Registrar usuario en `POST /api/auth/register`.
2. Iniciar sesión en `POST /api/auth/login`.
3. Copiar el token devuelto.
4. Enviar el token como Bearer Token en las rutas protegidas.

`auth-server` implementa una autenticación JWT formativa. OAuth 2.1 real no está implementado en esta entrega.

Los servicios de negocio no validan JWT individualmente. En esta entrega se asume que la entrada se realiza mediante el Gateway. En un entorno real, los puertos internos no deberían exponerse públicamente.

## 11. Swagger/OpenAPI

Cada servicio REST expone su propia documentación Swagger.

| Servicio | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| `auth-server` | [http://localhost:9000/swagger-ui/index.html](http://localhost:9000/swagger-ui/index.html) | [http://localhost:9000/v3/api-docs](http://localhost:9000/v3/api-docs) |
| `cliente-service` | [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |
| `cuenta-service` | [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) |
| `operacion-service` | [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html) | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) |

El Gateway no agrega Swagger. Swagger se usa para revisar cada microservicio de forma individual. Postman se usa para validar el flujo funcional completo a través del Gateway.

## 12. Endpoints Principales

Las siguientes rutas están pensadas para consumirse desde el Gateway:

```text
http://localhost:8080
```

### Autenticación

| Método | Endpoint | Acceso | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Público | Registra usuario. |
| `POST` | `/api/auth/login` | Público | Genera token JWT. |
| `GET` | `/api/auth/validate` | Público | Valida token JWT. |

### Clientes

| Método | Endpoint | Acceso | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/clientes` | Protegido | Lista clientes. |
| `POST` | `/api/clientes` | Protegido | Crea cliente. |
| `GET` | `/api/clientes/{id}` | Protegido | Obtiene cliente por ID. |
| `GET` | `/api/clientes/dni/{dni}` | Protegido | Obtiene cliente por DNI. |
| `PUT` | `/api/clientes/{id}` | Protegido | Actualiza cliente. |

### Cuentas

| Método | Endpoint | Acceso | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/cuentas` | Protegido | Crea cuenta. |
| `GET` | `/api/cuentas/{id}` | Protegido | Obtiene cuenta por ID. |
| `GET` | `/api/cuentas/numero/{numeroCuenta}` | Protegido | Obtiene cuenta por número. |
| `GET` | `/api/cuentas/{id}/saldo` | Protegido | Consulta saldo. |
| `GET` | `/api/cuentas/cliente/{clienteId}` | Protegido | Lista cuentas de un cliente. |

### Operaciones

| Método | Endpoint | Acceso | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/operaciones/deposito` | Protegido | Realiza depósito. |
| `POST` | `/api/operaciones/retiro` | Protegido | Realiza retirada. |
| `POST` | `/api/operaciones/transferencia` | Protegido | Realiza transferencia. |
| `GET` | `/api/operaciones/cuentas/{cuentaId}/movimientos` | Protegido | Lista movimientos. |
| `GET` | `/api/operaciones/cuentas/{cuentaId}/movimientos?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD` | Protegido | Lista movimientos por fechas. |

### Endpoints Internos De Cuenta-Service

Estos endpoints son consumidos por `operacion-service`. No forman parte del flujo principal de usuario.

| Método | Endpoint interno | Uso |
| --- | --- | --- |
| `POST` | `/internal/cuentas/{id}/depositos` | Incrementa saldo. |
| `POST` | `/internal/cuentas/{id}/retiros` | Reduce saldo. |
| `POST` | `/internal/cuentas/transferencias` | Ejecuta transferencia entre cuentas. |

## 13. Comunicación Entre Servicios

La comunicación síncrona se realiza mediante OpenFeign.

### cuenta-service -> cliente-service

`cuenta-service` llama a `cliente-service` para validar que un cliente existe antes de crear una cuenta.

```text
POST /api/cuentas
        |
        v
cuenta-service
        |
        v
cliente-service
```

`cuenta-service` no guarda una copia del cliente. Solo conserva `clienteId` como referencia lógica.

### operacion-service -> cuenta-service

`operacion-service` llama a `cuenta-service` para aplicar depósitos, retiradas y transferencias.

```text
POST /api/operaciones/deposito
        |
        v
operacion-service
        |
        v
cuenta-service
        |
        v
operacion-service registra movimiento
```

`operacion-service` no accede directamente a la base de datos de cuentas.

## 14. Resiliencia

Las llamadas Feign incorporan Resilience4j:

- Circuit Breaker.
- Retry.
- Fallbacks controlados.

Los fallbacks no inventan datos ni esconden errores. Su función es transformar fallos remotos en excepciones controladas para evitar respuestas inconsistentes.

Escenarios contemplados:

- `cliente-service` no encuentra un cliente.
- `cliente-service` devuelve error remoto.
- `cuenta-service` no encuentra una cuenta.
- `cuenta-service` rechaza una retirada por saldo insuficiente.
- `cuenta-service` no está disponible.

## 15. Persistencia Y Separación De Datos

Cada servicio mantiene su propia base de datos. No se usan foreign keys reales entre bases de datos de servicios distintos.

Ejemplos:

- `cuentas.cliente_id` referencia lógicamente a un cliente, pero no tiene FK real hacia `novabank_clientes`.
- `movimientos.cuenta_id` referencia lógicamente a una cuenta, pero no tiene FK real hacia `novabank_cuentas`.

Esta decisión evita acoplar físicamente los esquemas de distintos microservicios.

Responsabilidad de cuentas y movimientos:

- `cuenta-service` es dueño de cuentas, saldos y generación del número de cuenta.
- `operacion-service` es dueño de movimientos e historial financiero.
- `cuenta-service` no guarda movimientos.
- `operacion-service` no modifica saldos directamente en base de datos.

## 16. Testing

Ejecutar todos los tests:

```powershell
mvn clean test
```

Ejecutar tests por módulo:

```powershell
mvn -pl api-gateway test
mvn -pl auth-server test
mvn -pl cliente-service test
mvn -pl cuenta-service test
mvn -pl operacion-service test
```

La validación final conocida terminó con `BUILD SUCCESS`.

| Módulo | Tests |
| --- | ---: |
| `api-gateway` | 17 |
| `auth-server` | 27 |
| `cliente-service` | 29 |
| `cuenta-service` | 47 |
| `operacion-service` | 44 |
| **Total** | **164** |

La suite cubre:

- tests de controlador con MockMvc;
- tests de servicio con JUnit 5 y Mockito;
- tests de repositorio con H2;
- tests de contrato HTTP con WireMock;
- tests de fallback;
- tests del filtro JWT en Gateway;
- validaciones de JSON malformado, cuerpos inválidos y parámetros ausentes.

Los tests automatizados no requieren que todos los microservicios estén arrancados. Las dependencias remotas se sustituyen por mocks, stubs o WireMock según el tipo de prueba.

## 17. Postman

Postman se utiliza para validar el flujo funcional principal mediante el Gateway.

Variable recomendada:

```text
baseUrl = http://localhost:8080
```

Flujo recomendado:

1. Registrar usuario.
2. Hacer login.
3. Guardar token JWT.
4. Probar acceso protegido sin token.
5. Probar acceso protegido con token.
6. Crear cliente.
7. Consultar cliente.
8. Crear cuenta.
9. Consultar saldo.
10. Realizar depósito.
11. Realizar retirada.
12. Realizar transferencia.
13. Consultar historial de movimientos.
14. Filtrar movimientos por rango de fechas.

Casos negativos recomendados:

- Acceso sin token: `401`.
- Token inválido: `401`.
- Cliente inexistente: `404`.
- Datos inválidos: `400`.
- Saldo insuficiente: `422`.
- Servicio remoto no disponible: respuesta controlada según el caso.

## 18. Manejo De Errores

Cada servicio contiene un `GlobalExceptionHandler` para transformar excepciones en respuestas HTTP controladas.

Formato general:

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Recurso no encontrado",
  "service": "cliente-service",
  "timestamp": "2026-05-06T12:00:00"
}
```

Para errores de validación pueden incluirse errores por campo.

| Estado HTTP | Código | Uso |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | Datos de entrada inválidos. |
| `400` | `BAD_REQUEST` | JSON malformado o parámetro obligatorio ausente. |
| `401` | `UNAUTHORIZED` | Token ausente o inválido. |
| `404` | `RESOURCE_NOT_FOUND` | Recurso inexistente. |
| `409` | `CONFLICT` | Conflicto de unicidad o estado. |
| `422` | `INSUFFICIENT_BALANCE` | Saldo insuficiente. |
| `500` | `INTERNAL_ERROR` | Error inesperado. |
| `503` | `SERVICE_UNAVAILABLE` | Servicio remoto no disponible. |

Durante la ampliación de tests se corrigió el tratamiento de JSON malformado y parámetros obligatorios ausentes para devolver `400` en lugar de `500`.

## 19. Decisiones Técnicas Relevantes

### Migración Desde Monolito A Microservicios

El monolito del Módulo 3 se usó como base funcional. En el Módulo 4 se extrajeron servicios independientes por dominio.

La migración no cambia el dominio principal, pero sí la forma de organizarlo, desplegarlo y comunicarlo.

### Configuración Centralizada

La configuración se sirve desde `config-server` usando un repositorio Git local externo. Esto evita duplicar propiedades en cada servicio y permite simular un entorno de configuración centralizada.

### Gateway Como Punto De Entrada

`api-gateway` centraliza el acceso externo. También valida tokens JWT delegando en `auth-server`.

### JWT Formativo

La entrega mantiene JWT formativo. OAuth 2.1 real queda fuera del alcance final.

### Sin X-Correlation-Id

Durante la iteración se eliminaron residuos de `X-Correlation-Id` porque no formaban parte del alcance final.

### Sin Idempotencia Persistida

No se incluye:

- `Idempotency-Key`;
- `OperacionIdempotente`;
- `requestHash`;
- tabla de operaciones idempotentes.

La rama `develop-posibles-features` queda como exploración técnica de mejoras no integradas.


## 20. Limitaciones Conocidas

La versión final cumple el alcance formativo del Módulo 4, pero conserva limitaciones razonables:

- No existe transacción distribuida entre `cuenta-service` y `operacion-service`.
- Si el saldo se actualiza correctamente pero falla el registro del movimiento, no existe compensación automática.
- No se implementa OAuth 2.1 real.
- No se implementa Kafka, RabbitMQ ni mensajería asíncrona.
- No se implementa patrón SAGA.
- No se incluye Docker.
- Swagger no está agregado desde el Gateway.
- Los servicios internos no validan JWT individualmente.
- En un entorno real, los puertos internos deberían quedar cerrados o restringidos.

## 21. Mejoras Futuras

Posibles ampliaciones:

- OAuth 2.1 real con proveedor de identidad dedicado.
- Agregación de Swagger/OpenAPI en Gateway.
- Docker Compose para levantar todo el ecosistema.
- Mensajería asíncrona para eventos financieros.
- Patrón SAGA para operaciones distribuidas.
- Trazabilidad distribuida.
- Observabilidad con métricas, logs centralizados y tracing.
- Seguridad interna entre microservicios.
- Roles y permisos más detallados.
- Paginación en listados.
- Auditoría avanzada de operaciones.

## 22. Entrega Limpia

Antes de preparar una entrega comprimida:

```powershell
mvn clean test
mvn clean
```

Para generar un ZIP limpio desde Git:

```powershell
git archive --format=zip --output NovaBank-Modulo4.zip HEAD
```

`git archive` evita incluir:

- `.git/`;
- `target/`;
- ficheros compilados;
- restos de ejecuciones anteriores.

## 23. Repositorio

Repositorio público del proyecto:

[https://github.com/CroquetaConQueso/NovaBank](https://github.com/CroquetaConQueso/NovaBank)

## 24. Autor

Carlos Torres León
