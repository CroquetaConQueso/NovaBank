# NovaBank Digital Services - Modulo 4

NovaBank Digital Services es una evolucion del monolito Spring Boot del Modulo 3 hacia una arquitectura de microservicios sincronicos con Spring Cloud.

La version final del Modulo 4 separa las responsabilidades principales en servicios independientes, mantiene comunicacion HTTP mediante Feign, usa Eureka para descubrimiento, Config Server para configuracion centralizada y API Gateway como punto unico de entrada.

## Arquitectura general

- `eureka-server`: registro y descubrimiento de servicios. Puerto `8761`.
- `config-server`: servidor de configuracion centralizada desde un repositorio Git local externo. Puerto `8888`.
- `api-gateway`: punto de entrada HTTP del sistema. Puerto `8080`.
- `auth-server`: registro, login y validacion JWT formativa. Puerto `9000`.
- `cliente-service`: gestion de clientes. Puerto `8081`.
- `cuenta-service`: gestion de cuentas, saldos, generacion de numero de cuenta, `@Version` y `account_number_sequence`. Puerto `8082`.
- `operacion-service`: depositos, retiradas, transferencias e historial de movimientos. Puerto `8083`.

El monolito antiguo ya no forma parte del reactor Maven final.

## Tecnologias utilizadas

- Java 17
- Spring Boot 3.3.6
- Spring Cloud 2023.0.4
- Maven multi-modulo
- Spring Cloud Netflix Eureka
- Spring Cloud Config Server
- Spring Cloud Gateway
- OpenFeign
- Resilience4j
- Spring Data JPA
- PostgreSQL
- H2 para tests
- WireMock para contratos HTTP
- JUnit 5
- Mockito
- MockMvc
- Swagger/OpenAPI con `springdoc-openapi`

## Estructura del proyecto

```text
.
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

El repositorio `config-repo` no debe estar versionado dentro de este monorepo. La configuracion centralizada se sirve desde un repositorio Git local externo.

## Requisitos previos

- Java 17.
- Maven.
- PostgreSQL.
- Git.
- Puertos disponibles: `8761`, `8888`, `8080`, `9000`, `8081`, `8082`, `8083`.

Comprobacion recomendada:

```powershell
java -version
mvn -version
```

## Base de datos

PostgreSQL debe tener las siguientes bases:

- `novabank_auth`
- `novabank_clientes`
- `novabank_cuentas`
- `novabank_operaciones`

Los scripts SQL de referencia estan en `docs/sql`:

- `create-databases.sql`
- `novabank_auth_schema.sql`
- `novabank_clientes_schema.sql`
- `novabank_cuentas_schema.sql`
- `novabank_operaciones_schema.sql`

Los servicios usan `ddl-auto: validate`. Las bases y tablas deben existir antes de arrancar los servicios. Las credenciales se configuran en el repositorio Git local externo de configuracion y deben ajustarse a cada entorno local.

## Config Server

`config-server` esta configurado para leer desde:

```text
file:///C:/Users/Usuario/Desktop/config-repo
```

Ese directorio debe ser un repositorio Git local externo, inicializado en la rama `main` y con al menos un commit.

Contenido esperado:

```text
config-repo/
|-- application.yml
|-- api-gateway.yml
|-- auth-server.yml
|-- cliente-service.yml
|-- cuenta-service.yml
|-- operacion-service.yml
```

Ejemplo de inicializacion del repositorio externo:

```powershell
cd C:\Users\Usuario\Desktop\config-repo
git init
git add .
git commit -m "Configuracion inicial de microservicios NovaBank"
```

No se debe copiar ni versionar `config-repo` dentro del proyecto principal.

## Orden de arranque

Ejecutar cada servicio desde la raiz del proyecto en terminales separadas:

```powershell
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl auth-server spring-boot:run
mvn -pl cliente-service spring-boot:run
mvn -pl cuenta-service spring-boot:run
mvn -pl operacion-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Orden recomendado:

1. `eureka-server`
2. `config-server`
3. `auth-server`
4. `cliente-service`
5. `cuenta-service`
6. `operacion-service`
7. `api-gateway`

## Pruebas de funcionamiento

- Eureka: [http://localhost:8761](http://localhost:8761)
- Config Server: [http://localhost:8888/cliente-service/default](http://localhost:8888/cliente-service/default)
- Gateway: [http://localhost:8080](http://localhost:8080)

El flujo funcional principal debe probarse mediante el Gateway:

```text
baseUrl = http://localhost:8080
```

Flujo recomendado:

1. Registrar usuario en `POST /api/auth/register`.
2. Obtener token en `POST /api/auth/login`.
3. Enviar `Authorization: Bearer <token>` en las rutas de negocio.

## Swagger/OpenAPI

Cada servicio REST expone su propia documentacion:

- Auth Server: [http://localhost:9000/swagger-ui/index.html](http://localhost:9000/swagger-ui/index.html)
- Auth API Docs: [http://localhost:9000/v3/api-docs](http://localhost:9000/v3/api-docs)
- Cliente Service: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- Cliente API Docs: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)
- Cuenta Service: [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)
- Cuenta API Docs: [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs)
- Operacion Service: [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html)
- Operacion API Docs: [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs)

El Gateway no agrega Swagger. Swagger se consulta directamente por servicio y Postman se usa para probar el flujo funcional a traves del Gateway.

## Endpoints principales

### Autenticacion

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/validate`

### Clientes

- `GET /api/clientes`
- `POST /api/clientes`
- `GET /api/clientes/{id}`
- `GET /api/clientes/dni/{dni}`
- `PUT /api/clientes/{id}`

### Cuentas

- `POST /api/cuentas`
- `GET /api/cuentas/{id}`
- `GET /api/cuentas/numero/{numeroCuenta}`
- `GET /api/cuentas/{id}/saldo`
- `GET /api/cuentas/cliente/{clienteId}`

### Operaciones

- `POST /api/operaciones/deposito`
- `POST /api/operaciones/retiro`
- `POST /api/operaciones/transferencia`
- `GET /api/operaciones/cuentas/{cuentaId}/movimientos`
- `GET /api/operaciones/cuentas/{cuentaId}/movimientos?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD`

### Endpoints internos

Los siguientes endpoints pertenecen a `cuenta-service` y son consumidos por `operacion-service`. No forman parte del flujo principal de usuario:

- `POST /internal/cuentas/{id}/depositos`
- `POST /internal/cuentas/{id}/retiros`
- `POST /internal/cuentas/transferencias`

## Seguridad

El Gateway actua como frontera de entrada:

- `/api/auth/login`, `/api/auth/register` y `/api/auth/validate` son rutas publicas.
- `/api/clientes/**`, `/api/cuentas/**` y `/api/operaciones/**` requieren `Authorization: Bearer <token>`.
- El Gateway valida el token consultando a `auth-server`.
- `auth-server` implementa JWT formativo.
- OAuth 2.1 real no esta implementado en esta entrega.

Los servicios de negocio no validan JWT individualmente. En un despliegue real, sus puertos internos no deberian exponerse publicamente.

## Comunicacion entre servicios

- `cuenta-service` llama a `cliente-service` para validar que un cliente existe antes de crear cuentas.
- `operacion-service` llama a `cuenta-service` para aplicar depositos, retiradas y transferencias sobre saldos.
- Las llamadas sincronicas usan OpenFeign, Eureka y Resilience4j.
- Los fallbacks evitan exponer excepciones remotas crudas al cliente.

## Testing

Comando general:

```powershell
mvn clean test
```

Tests por modulo:

```powershell
mvn -pl api-gateway test
mvn -pl auth-server test
mvn -pl cliente-service test
mvn -pl cuenta-service test
mvn -pl operacion-service test
```

La ultima validacion local conocida fue `BUILD SUCCESS` con `164` tests:

- `api-gateway`: 17
- `auth-server`: 27
- `cliente-service`: 29
- `cuenta-service`: 47
- `operacion-service`: 44

La suite usa H2 para persistencia de test, MockMvc para controladores MVC, WebTestClient en Gateway, WireMock para contratos HTTP, Mockito y JUnit 5.

## Decisiones de diseno

- `cuenta-service` es duenio de cuentas, saldos y generacion de numero de cuenta.
- `operacion-service` es duenio de movimientos e historial de operaciones.
- `cuenta-service` no guarda movimientos.
- `operacion-service` no accede directamente a la base de datos de cuentas.
- Las referencias entre servicios son logicas; no hay foreign keys reales entre bases de datos de servicios distintos.
- No hay idempotencia persistida.
- No hay `X-Correlation-Id`.
- Config Server usa un repositorio Git local externo.
- `develop-posibles-features` queda como rama de exploracion de mejoras no integradas.

## Limitaciones conocidas

- No existe transaccion distribuida entre `cuenta-service` y `operacion-service`.
- No se implementa OAuth 2.1 real.
- No se usa Kafka, SAGA, Docker ni Docker Compose.
- Los servicios internos deberian quedar detras del Gateway en un entorno real.
- La seguridad por servicio queda fuera del alcance formativo de esta entrega.

## Repositorio

[https://github.com/CroquetaConQueso/NovaBank](https://github.com/CroquetaConQueso/NovaBank)
