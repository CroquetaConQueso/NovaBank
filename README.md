# NovaBank Digital Services - Módulo 3

NovaBank Digital Services es un backend bancario desarrollado como API REST con Spring Boot.

En este módulo, el proyecto evoluciona desde la aplicación de consola con JDBC manual del Módulo 2 hacia un servicio HTTP profesional basado en Spring Boot, Spring Data JPA, Spring Security, JWT, OpenAPI/Swagger, DTOs y testing por capas.

La lógica de negocio principal se mantiene respecto a los módulos anteriores: gestión de clientes, cuentas, operaciones financieras y movimientos. Lo que cambia es el canal de acceso y la infraestructura técnica: desaparece la consola y el sistema pasa a exponerse mediante endpoints REST protegidos con autenticación JWT.

## 1. Descripción del proyecto

NovaBank Digital Services simula una entidad bancaria básica con las siguientes capacidades:

- Gestión de clientes.
- Gestión de cuentas bancarias.
- Operaciones financieras:
  - depósito;
  - retirada;
  - transferencia entre cuentas.
- Consulta de saldo.
- Consulta de movimientos.
- Filtrado de movimientos por rango de fechas.
- Autenticación mediante JWT.
- Documentación interactiva mediante Swagger UI.
- Pruebas automatizadas por capas.

El proyecto sigue una arquitectura por capas:

```text
Controller -> Service -> Repository -> JPA/PostgreSQL
```

Los controladores exponen la API HTTP, los servicios contienen la lógica de negocio, los repositorios gestionan la persistencia mediante Spring Data JPA y las entidades se mapean a tablas relacionales mediante JPA/Hibernate.

## 2. Tecnologías utilizadas

- Java 17
- Maven
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security
- JWT con JJWT
- PostgreSQL
- H2 para tests y ejecución con perfil de prueba
- Bean Validation
- springdoc-openapi
- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc
- Lombok

## 3. Requisitos del sistema

Para ejecutar el proyecto en local se necesita:

- Java 17 o superior.
- Maven 3.6 o superior.
- PostgreSQL 14 o superior.
- Postman, opcional, para probar el flujo completo de la API.
- Git, opcional, para clonar el repositorio.

Comprobación recomendada:

```bash
java -version
mvn -version
```

El proyecto está configurado para Java 17. Aunque una versión superior del JDK pueda compilar parte del código, se recomienda ejecutar Maven con JDK 17 para evitar problemas con procesadores de anotaciones como Lombok.

## 4. Configuración de la base de datos

La aplicación usa PostgreSQL por defecto.

La base de datos del módulo se llama:

```text
novabank
```

La configuración principal se encuentra en:

```text
src/main/resources/application.yml
```

La aplicación permite configurar la conexión mediante variables de entorno.

### Variables de entorno

Ejemplo en PowerShell:

```powershell
$env:NOVABANK_DB_URL="jdbc:postgresql://localhost:5432/novabank"
$env:NOVABANK_DB_USER="postgres"
$env:NOVABANK_DB_PASSWORD="usuario"
$env:JWT_SECRET="novabank-secret-key-for-jwt-generation-2026"
$env:JWT_EXPIRATION="86400000"
```

Valores por defecto principales:

```text
NOVABANK_DB_URL=jdbc:postgresql://localhost:5432/novabank
NOVABANK_DB_USER=postgres
NOVABANK_DB_PASSWORD=usuario
JWT_EXPIRATION=86400000
```

### Preparar PostgreSQL

La configuración productiva usa:

```yaml
spring.jpa.hibernate.ddl-auto: validate
spring.sql.init.mode: never
```

Esto significa que Spring Boot no crea automáticamente la base de datos ni las tablas. Hibernate solo valida que el esquema existente coincide con las entidades JPA.

Orden recomendado:

1. Crear la base de datos con:

```text
docs/sql/create-database.sql
```

2. Ejecutar el script de tablas sobre la base `novabank`:

```text
src/main/resources/schema.sql
```

3. Configurar las variables de entorno de conexión.

4. Arrancar la aplicación.

El archivo `schema.sql` contiene la estructura de tablas, constraints e índices. No incluye `CREATE DATABASE` ni comandos propios de `psql`.

## 5. Cómo ejecutar la aplicación

Ejecutar la aplicación con PostgreSQL:

```bash
mvn spring-boot:run
```

La aplicación arranca por defecto en:

```text
http://localhost:8080
```

### Ejecución con H2 usando el perfil de test

También se puede arrancar usando H2 con el perfil `test`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.useTestClasspath=true
```

Esta opción es útil para probar el backend sin depender de una instancia local de PostgreSQL.

## 6. Autenticación JWT

La API protege sus endpoints mediante JWT.

El endpoint de autenticación es público:

```http
POST /api/auth/login
```

Usuario de prueba del módulo:

```text
username: admin
password: password
```

Ejemplo de petición:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

Ejemplo de respuesta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiracion": 86400000
}
```

Para consumir endpoints protegidos se debe enviar el token en el header:

```http
Authorization: Bearer <token>
```

Ejemplo:

```http
GET /api/clientes
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## 7. Documentación de la API con Swagger

La documentación interactiva está disponible con la aplicación arrancada:

```text
http://localhost:8080/swagger-ui.html
```

La especificación OpenAPI en formato JSON está disponible en:

```text
http://localhost:8080/v3/api-docs
```

Los endpoints de autenticación y Swagger son públicos. El resto de endpoints requiere token JWT.

Para probar endpoints protegidos desde Swagger UI:

1. Obtener un token desde `POST /api/auth/login`.
2. Pulsar el botón `Authorize`.
3. Introducir:

```text
Bearer <token>
```

4. Ejecutar las peticiones protegidas desde Swagger.

## 8. Cómo ejecutar los tests

Ejecutar todos los tests:

```bash
mvn test
```

Ejecución limpia completa:

```bash
mvn clean test
```

La suite de pruebas cubre varios niveles:

- Tests unitarios de servicios con JUnit 5 y Mockito.
- Tests de repositorio con Spring Data JPA.
- Tests de controlador con `@WebMvcTest` y MockMvc.
- Tests de seguridad.
- Tests de integración end-to-end con Spring Boot Test.

Los tests usan H2 en perfil `test`, lo que permite probar repositorios e integración sin depender de PostgreSQL real.

## 9. Arquitectura del sistema

El proyecto usa arquitectura por capas.

```text
com.novabank
|-- NovaBankApplication
|-- config
|-- controller
|-- dto
|-- exception
|-- mapper
|   |-- contract
|-- model
|-- repository
|-- security
|-- service
|   |-- strategy
|-- validation
```

### Responsabilidades por paquete

- `config`: configuración general de Spring, seguridad y OpenAPI.
- `controller`: endpoints REST. Reciben peticiones HTTP, delegan en servicios y devuelven DTOs.
- `dto`: contratos públicos de entrada y salida. No contienen anotaciones JPA.
- `exception`: excepciones de dominio y `GlobalExceptionHandler`.
- `mapper`: conversión manual entre entidades JPA y DTOs.
- `mapper.contract`: interfaces pequeñas para mappers:
  - `RequestMapper`;
  - `ResponseMapper`.
- `model`: entidades JPA:
  - `Cliente`;
  - `Cuenta`;
  - `Movimiento`.
- `repository`: interfaces Spring Data JPA.
- `security`: JWT, filtro de autenticación y respuesta JSON para errores 401.
- `service`: lógica de negocio y transacciones declarativas con `@Transactional`.
- `service.strategy`: estrategia de generación del número de cuenta.
- `validation`: anotaciones y validadores personalizados, como `@ValidDni`.

## 10. Endpoints principales

| Método | Endpoint | Acceso | Descripción |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | Público | Genera token JWT |
| GET | `/api/clientes` | Protegido | Lista clientes |
| POST | `/api/clientes` | Protegido | Crea cliente |
| GET | `/api/clientes/{id}` | Protegido | Obtiene cliente por ID |
| GET | `/api/clientes?dni=...` | Protegido | Obtiene cliente por DNI |
| POST | `/api/cuentas` | Protegido | Crea cuenta |
| GET | `/api/cuentas/{id}` | Protegido | Obtiene cuenta por ID |
| GET | `/api/cuentas/numero/{numeroCuenta}` | Protegido | Obtiene cuenta por número |
| GET | `/api/cuentas/{id}/saldo` | Protegido | Consulta saldo de cuenta |
| GET | `/api/clientes/{clienteId}/cuentas` | Protegido | Lista cuentas de cliente |
| POST | `/api/operaciones/deposito` | Protegido | Registra depósito |
| POST | `/api/operaciones/retiro` | Protegido | Registra retirada |
| POST | `/api/operaciones/transferencia` | Protegido | Registra transferencia |
| GET | `/api/cuentas/{id}/movimientos` | Protegido | Lista movimientos de una cuenta |
| GET | `/api/cuentas/{id}/movimientos?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD` | Protegido | Lista movimientos por rango de fechas |

## 11. Postman

La colección Postman se encuentra en:

```text
docs/postman/NovaBank-Modulo-3.postman_collection.json
```

La colección cubre el flujo principal de uso de la API:

1. Login.
2. Acceso sin token.
3. Acceso con token.
4. Creación de clientes.
5. Consulta de clientes.
6. Creación de cuentas.
7. Consulta de cuentas.
8. Consulta de saldo.
9. Operaciones financieras.
10. Consulta de movimientos.
11. Movimientos por rango de fechas.

También incluye casos negativos:

- Cliente sin token: `401`.
- DNI inválido: `400`.
- Cliente duplicado: `409`.
- Retiro con saldo insuficiente: `422`.

Nota: se recomienda ejecutar la colección sobre una base de datos limpia, ya que DNI, email y teléfono tienen restricciones de unicidad.

## 12. Manejo de errores API

La clase `GlobalExceptionHandler` centraliza las respuestas de error.

Ejemplo de respuesta estándar:

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Cuenta no encontrada",
  "timestamp": "2026-04-26T12:00:00"
}
```

Para errores de validación se incluye además `fieldErrors`.

Ejemplo:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Error de validación",
  "timestamp": "2026-04-26T12:00:00",
  "fieldErrors": {
    "dni": "El DNI no es valido"
  }
}
```

Códigos principales:

| Estado HTTP | Código | Descripción |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Error de validación Bean Validation |
| 400 | `BAD_REQUEST` | Datos inválidos |
| 401 | `UNAUTHORIZED` | Token ausente, inválido o credenciales incorrectas |
| 404 | `RESOURCE_NOT_FOUND` | Recurso no encontrado |
| 409 | `CONFLICT` | DNI, email, teléfono u otro dato único duplicado |
| 409 | `CONCURRENT_MODIFICATION` | Conflicto de concurrencia optimista |
| 422 | `SALDO_INSUFICIENTE` | Saldo insuficiente para una operación |
| 500 | `INTERNAL_ERROR` | Error inesperado |

## 13. Decisiones técnicas relevantes

### Migración desde JDBC a Spring Data JPA

El Módulo 2 usaba JDBC manual, `Connection`, `PreparedStatement`, `ResultSet`, `commit` y `rollback`.

En el Módulo 3, ese código se sustituye por:

- entidades JPA;
- repositorios Spring Data JPA;
- transacciones declarativas con `@Transactional`;
- configuración centralizada mediante Spring Boot.

En el flujo principal ya no se usa:

- `DatabaseConnectionManager`;
- `RepositoryFactory`;
- `Connection`;
- `PreparedStatement`;
- `ResultSet`;
- `commit`;
- `rollback`.

Spring y Hibernate gestionan la persistencia y las transacciones.

### DTOs separados para entrada y salida

La API no expone entidades JPA directamente.

Se utilizan DTOs para separar:

- datos recibidos en requests;
- datos devueltos en responses;
- modelo interno persistido mediante JPA.

Esto evita exponer relaciones internas, reduce riesgo de ciclos de serialización y permite evolucionar la API sin acoplarla directamente a las entidades.

### Mappers manuales

Los mappers transforman entidades en DTOs y DTOs en entidades.

Se usan interfaces pequeñas para definir contratos de conversión:

- `ResponseMapper<E, R>`;
- `RequestMapper<D, E>`.

No se usa una interfaz genérica única que obligue a todos los mappers a implementar métodos que no necesitan.

### Validación personalizada de DNI

El proyecto incluye la anotación personalizada:

```java
@ValidDni
```

Esta validación comprueba:

- formato de 8 dígitos y una letra;
- letra correcta del DNI español;
- aceptación de minúsculas normalizando internamente.

`@NotBlank` sigue siendo responsable de validar que el campo no esté vacío.

### Optimización de duplicados con `@Query`

La validación de duplicados de cliente se centraliza mediante una query en `ClienteRepository`.

Se buscan coincidencias por:

- DNI;
- email;
- teléfono.

Después, `ClienteService` mantiene el orden funcional de validación:

1. DNI.
2. Email.
3. Teléfono.

### Control de concurrencia optimista con `@Version`

La entidad `Cuenta` incluye un campo de versión:

```java
@Version
private Long version;
```

Esta decisión se aplica solo a `Cuenta` porque es la entidad que contiene el saldo y puede verse afectada por operaciones concurrentes.

Si Hibernate detecta que una cuenta fue modificada por otra transacción, se devuelve:

```text
409 CONCURRENT_MODIFICATION
```

### Generación de número de cuenta

La generación del número de cuenta se encapsula mediante una estrategia.

El formato funcional usado es:

```text
ES91210000 + 12 dígitos secuenciales
```

La estrategia actual es simple y suficiente para el módulo. Como mejora futura, podría sustituirse por una secuencia de base de datos si se quisiera soportar concurrencia alta.

### Fechas de creación

Las fechas de creación se asignan desde JPA/aplicación mediante callbacks como `@PrePersist`.

Los `DEFAULT CURRENT_TIMESTAMP` del esquema quedan como respaldo de base de datos.

### Relaciones JPA

Las relaciones JPA no usan `CascadeType.ALL` ni `orphanRemoval` en el flujo financiero para evitar borrados accidentales de histórico.

El histórico de movimientos debe conservarse como registro financiero.

### Usuario en memoria

Para este módulo se usa un usuario en memoria:

```text
admin/password
```

No se incluye todavía:

- entidad `Usuario`;
- `UsuarioRepository`;
- sistema de roles persistido.

Esto mantiene el alcance del módulo centrado en Spring Security, JWT y protección de endpoints.

## 14. Funcionalidades implementadas

### Clientes

- Crear cliente.
- Listar clientes.
- Obtener cliente por ID.
- Obtener cliente por DNI.
- Validar DNI, email y teléfono.
- Detectar duplicados.

### Cuentas

- Crear cuenta asociada a cliente.
- Obtener cuenta por ID.
- Obtener cuenta por número.
- Consultar saldo.
- Listar cuentas de un cliente.

### Operaciones financieras

- Depósito.
- Retirada.
- Transferencia entre cuentas.
- Control de saldo insuficiente.
- Registro automático de movimientos.

### Movimientos

- Listar movimientos de una cuenta.
- Filtrar movimientos por rango de fechas.
- Soportar tipos:
  - `DEPOSITO`;
  - `RETIRO`;
  - `TRANSFERENCIA_SALIENTE`;
  - `TRANSFERENCIA_ENTRANTE`.

### Seguridad

- Login con JWT.
- Protección de endpoints.
- Respuesta JSON para errores 401.
- Sesiones stateless.

### Documentación y pruebas

- Swagger UI.
- OpenAPI JSON.
- Colección Postman.
- Tests unitarios, de repositorio, controlador, seguridad e integración.

## 15. Entrega limpia

Antes de generar un ZIP de entrega:

```bash
mvn clean
git archive --format=zip --output NovaBank-Modulo3.zip HEAD
```

`git archive` genera una entrega de código fuente sin incluir:

- `.git/`;
- `target/`;
- ficheros compilados;
- reportes antiguos de tests.

Esto evita entregar artefactos generados o restos de ejecuciones anteriores.

## 16. Repositorio

Repositorio público del proyecto:

```text
https://github.com/CroquetaConQueso/NovaBank
```

## 17. Autor

Carlos Torres León
