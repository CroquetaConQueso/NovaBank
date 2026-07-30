# Docker cloud-native NovaBank

Esta configuracion levanta la plataforma local del Caso Practico 7 con Docker Compose, sin AWS real y sin depender del `config-repo` externo del escritorio.

## Servicios

| Servicio | Puerto host | Uso |
| --- | ---: | --- |
| PostgreSQL | 5432 | Base de datos local `novabank` |
| Kafka | 9094 | Broker accesible desde host |
| Kafka UI | 8090 | UI de Kafka |
| LocalStack | 4566 | S3 y Lambda locales |
| Eureka Server | 8761 | Service discovery |
| Config Server | 8888 | Configuracion nativa Docker |
| API Gateway | 8080 | Entrada HTTP principal |
| Auth Server | 9000 | Autenticacion |
| Cliente Service | 8081 | Clientes |
| Cuenta Service | 8082 | Cuentas y consumidor Kafka |
| Operacion Service | 8083 | Operaciones, SAGA y Lambda comisiones |
| Exchange Rate Mock Service | 8084 | Tipo de cambio mock |
| Notificacion Service | interno 8085 | Consumidores de notificaciones |
| Documento Service | 8086 | Documentos y S3 |

Todos los contenedores se conectan a `novabank-network`.

## Imagenes

Cada microservicio Spring Boot tiene Dockerfile multi-stage propio con:

- builder `maven:3.9-eclipse-temurin-17`;
- runtime `eclipse-temurin:17-jre`;
- usuario no root `novabank`;
- `JAVA_OPTS="-XX:MaxRAMPercentage=75.0"`;
- build context desde la raiz del repositorio.

El contexto debe ser la raiz porque Maven necesita el POM padre y modulos como `novabank-events`.

## Configuracion principal

Dentro de Docker se usan nombres de servicio:

| Recurso | Valor interno |
| --- | --- |
| Eureka | `http://eureka-server:8761/eureka/` |
| Config Server | `http://config-server:8888` |
| Kafka | `kafka:9092` |
| PostgreSQL | `postgres:5432` |
| LocalStack | `http://localstack:4566` |
| Lambda comisiones | `novabank-comision` |

Config Server arranca con perfil `native` y lee configuracion empaquetada en `config-server/src/main/resources/config-repo-docker`. No monta rutas absolutas ni usa el `config-repo` externo.

## Comandos

Construir imagenes:

```powershell
docker compose build
```

o:

```powershell
.\scripts\build-docker-images.ps1
```

Levantar la plataforma:

```powershell
docker compose up -d
```

o construyendo antes:

```powershell
.\scripts\start-local-platform.ps1 -Build
```

Ver estado:

```powershell
docker compose ps
```

Parar:

```powershell
docker compose down
```

Parar y limpiar volumenes locales:

```powershell
docker compose down -v
```

## Comprobaciones

Eureka:

```text
http://localhost:8761
```

Kafka UI:

```text
http://localhost:8090
```

LocalStack:

```powershell
aws --endpoint-url http://localhost:4566 s3 ls
```

API Gateway:

```text
http://localhost:8080/actuator/health
```

Swagger UI unificado:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI agregados:

```text
http://localhost:8080/v3/api-docs/auth-server
http://localhost:8080/v3/api-docs/cliente-service
http://localhost:8080/v3/api-docs/cuenta-service
http://localhost:8080/v3/api-docs/operacion-service
http://localhost:8080/v3/api-docs/exchange-rate-mock-service
http://localhost:8080/v3/api-docs/documento-service
```

`notificacion-service` no se agrega a Swagger porque no expone API REST publica.

## LocalStack

LocalStack se arranca con:

```text
SERVICES=s3,lambda
AWS_DEFAULT_REGION=eu-west-1
```

El bucket `novabank-justificantes` se crea con:

```text
localstack-init/01-crear-buckets.sh
```

montado como:

```text
/etc/localstack/init/ready.d/01-crear-buckets.sh
```

## Lambda de comisiones

El compose deja LocalStack preparado para Lambda, pero no despliega automaticamente `novabank-comision`. Se mantiene como paso manual para evitar un arranque fragil dependiente de que el JAR exista antes de `docker compose up`.

Despliegue manual:

```powershell
mvn -pl comision-lambda package
.\scripts\deploy-comision-lambda-localstack.ps1
```

Invocacion manual:

```powershell
.\scripts\invoke-comision-lambda-localstack.ps1
```

No se usan credenciales reales; los scripts y compose usan credenciales dummy `test`.

## Caveats

- Si Docker Desktop no esta arrancado, `docker compose build/up` fallara aunque las validaciones Maven pasen.
- Algunos tests de repositorio usan Testcontainers y requieren Docker Desktop.
- El compose usa una unica base de datos `novabank` para simplificar el entorno local; los `schema.sql` de cada servicio crean tablas con `IF NOT EXISTS`.
- Kafka publica internamente en `kafka:9092` y externamente para el host en `localhost:9094`.
