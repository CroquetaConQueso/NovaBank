# NovaBank Digital Services - Entregable Modulo 3

## 1. Objetivo del modulo

El Modulo 3 transforma NovaBank desde una aplicacion de consola con JDBC manual a un backend REST profesional accesible por HTTP.

La solucion implementa:

- API REST con Spring Boot.
- Persistencia con Spring Data JPA.
- DTOs separados entre Request y Response.
- Servicios transaccionales con `@Service` y `@Transactional`.
- Autenticacion con Spring Security y JWT.
- Documentacion OpenAPI/Swagger.
- Testing por capas.
- Limpieza del codigo legacy de consola y JDBC manual.

## 2. Arquitectura final

Paquete base:

```text
com.novabank
|-- config
|-- controller
|-- dto
|-- exception
|-- mapper
|-- model
|-- repository
|-- security
|-- service
    |-- strategy
```

Responsabilidades:

- `controller`: expone los endpoints REST.
- `dto`: define contratos publicos de entrada y salida.
- `mapper`: convierte entidades JPA a DTOs y viceversa.
- `model`: contiene entidades JPA.
- `repository`: usa Spring Data JPA.
- `service`: concentra reglas de negocio y transacciones.
- `security`: contiene JWT, filtro y configuracion de Spring Security.
- `exception`: centraliza excepciones y respuestas de error.

## 3. Persistencia y transacciones

La persistencia se realiza mediante repositorios Spring Data JPA:

- `ClienteRepository`
- `CuentaRepository`
- `MovimientoRepository`

Las operaciones de negocio se ejecutan desde servicios transaccionales:

- `ClienteService`
- `CuentaService`
- `OperacionService`

No hay uso de JDBC manual en el flujo principal. Se eliminaron las piezas antiguas del Modulo 2 como `DatabaseConnectionManager`, `RepositoryFactory`, conexiones manuales, `PreparedStatement`, `ResultSet`, `commit` y `rollback`.

La configuracion del perfil `test` se encuentra en `src/test/resources/application-test.yml`, por lo que no se empaqueta como recurso productivo.

`schema.sql` describe la estructura de tablas, constraints e indices. La creacion manual de base de datos queda separada en `docs/sql/create-database.sql`.

Las fechas se asignan desde JPA/aplicacion con callbacks `@PrePersist`. Los `DEFAULT CURRENT_TIMESTAMP` del esquema se mantienen solo como respaldo.

## 4. DTOs y mappers

Los RequestDTO reciben datos de entrada y aplican validaciones con `jakarta.validation`.

Validación destacada:
- `@ValidDni` sustituye la validación superficial del DNI.
- Valida formato y letra real del DNI español (8 dígitos + letra correcta).
- `@NotBlank` sigue cubriendo obligatoriedad.

Los ResponseDTO definen lo que se expone al cliente HTTP.

DTOs principales:

- `ClienteRequestDTO`
- `ClienteResponseDTO`
- `CuentaCreateRequestDTO`
- `CuentaResponseDTO`
- `SaldoResponseDTO`
- `OperacionRequestDTO`
- `TransferenciaRequestDTO`
- `MovimientoResponseDTO`
- `LoginRequestDTO`
- `LoginResponseDTO`
- `ErrorResponseDTO`

Los mappers son manuales y no acceden a repositorios.

Contratos de mappers (ISP):
- `ResponseMapper<E, R>` define conversión entidad → response DTO.
- `RequestMapper<D, E>` define conversión request DTO → entidad.
No se usa una interfaz genérica gigante para evitar obligar a métodos innecesarios.

## 5. Seguridad

La autenticacion se implementa con Spring Security y JWT.

Componentes:

- `SecurityConfig`
- `JwtService`
- `JwtFilter`
- `JsonAuthenticationEntryPoint`
- `AuthController`

Decisiones:

- Usuario en memoria: `admin/password`.
- PasswordEncoder con BCrypt.
- `/api/auth/**` y Swagger son publicos.
- El resto de endpoints requiere token JWT.
- No se crea entidad `Usuario` ni repositorio de usuarios en este modulo.
- Los errores 401 se devuelven en JSON mediante `JsonAuthenticationEntryPoint`.

## 6. Manejo global de errores

`GlobalExceptionHandler` usa `@RestControllerAdvice` y metodos `@ExceptionHandler` para evitar try/catch en controladores.

Errores cubiertos:

- `ResourceNotFoundException`: 404.
- `InsufficientBalanceException`: 422.
- `IllegalArgumentException`: 400.
- `MethodArgumentNotValidException`: 400 con `fieldErrors`.
- `DuplicateResourceException`: 409.
- `AuthenticationException`: 401.
- `Exception`: 500.

Las respuestas usan `ErrorResponseDTO` con:

- `code`
- `message`
- `timestamp`
- `fieldErrors`, cuando aplica.

## 7. Endpoints principales

- `POST /api/auth/login`

Clientes:
- `POST /api/clientes`
- `GET /api/clientes`
- `GET /api/clientes/{id}`
- `GET /api/clientes?dni=...` (compatibilidad con el flujo del Módulo 1)

Cuentas:
- `POST /api/cuentas`
- `GET /api/cuentas/{id}`
- `GET /api/cuentas/numero/{numeroCuenta}`
- `GET /api/clientes/{clienteId}/cuentas`
- `GET /api/cuentas/{id}/saldo`

Operaciones:
- `POST /api/operaciones/deposito`
- `POST /api/operaciones/retiro`
- `POST /api/operaciones/transferencia`

Movimientos:
- `GET /api/cuentas/{id}/movimientos`
- `GET /api/cuentas/{id}/movimientos?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD`

Los numeros de cuenta se generan con formato `ES91210000` + 12 digitos secuenciales. La estrategia actual es simple; como mejora futura se recomienda sustituirla por una secuencia de base de datos si se requiere robustez ante concurrencia alta.

Las relaciones JPA entre clientes, cuentas y movimientos no usan cascadas agresivas ni `orphanRemoval`, para evitar borrado accidental de historico financiero.

### Duplicados (Cliente)
La validación de duplicados de `Cliente` se optimiza mediante una única consulta en repositorio (`@Query`) que detecta coincidencias por:
- DNI
- email
- teléfono

Después el servicio mantiene el orden funcional del mensaje de conflicto:
1. DNI
2. email
3. teléfono

### Concurrencia (Cuenta)
- `@Version` se añade solo en `Cuenta` porque es la entidad que contiene saldo.
- Hibernate detecta conflictos concurrentes (optimistic locking).
- La API devuelve `409 CONCURRENT_MODIFICATION` cuando detecta modificación concurrente.

## 8. Testing por capas

La suite incluye:

- Tests de repositorio con `@DataJpaTest`.
- Tests unitarios de servicios con Mockito.
- Tests web con MockMvc.
- Tests de seguridad e integracion con `@SpringBootTest`.
- Flujo end-to-end autenticado.

Validacion final:

```bash
mvn clean test
```

El flujo end-to-end prueba login, cliente, cuenta, deposito, retirada, transferencia, saldo insuficiente y consulta de movimientos.

## 9. Documentacion y herramientas

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- Coleccion Postman: `docs/postman/NovaBank-Modulo-3.postman_collection.json`

La colección Postman cubre:
- login y token JWT
- flujo de clientes/cuentas/operaciones/movimientos
- casos negativos (DNI inválido, duplicados, saldo insuficiente)

Nota: recomendado ejecutar la colección sobre base de datos limpia, ya que DNI/email/teléfono son únicos.

## 10. SOLID aplicado (con clases reales)

- **SRP**: `ClienteController`, `CuentaController`, `OperacionController` se limitan a HTTP; `ClienteService`, `CuentaService`, `OperacionService` concentran negocio; `ClienteRepository`, `CuentaRepository`, `MovimientoRepository` se limitan a persistencia.
- **ISP**: `ResponseMapper` y `RequestMapper` separan contratos pequeños de conversión.
- **DIP**: servicios dependen de repositorios (interfaces Spring Data) y de estrategias (`GeneradorNumeroCuentaStrategy`).
- **OCP**: la validación de DNI se añade mediante `@ValidDni` sin cambiar controladores.
- **LSP**: los contratos `ResponseMapper`/`RequestMapper` permiten múltiples implementaciones (mappers) sin afectar a consumidores.

## 11. Estado final

El modulo queda preparado como API REST Spring Boot, con el codigo legacy de consola/JDBC eliminado del flujo principal y con validacion automatizada mediante Maven.
