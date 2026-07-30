# Reporte de validacion final - Modulo 7

## Contexto

| Campo | Valor |
| --- | --- |
| Fecha | 2026-07-30 |
| Rama | `feature/m7-final-stabilization` |
| Commit base validado | `9dc77f9` |
| Ultimo commit | `9dc77f9 Merge pull request #166 from CroquetaConQueso/feature/m7-dockerhub-docs-delivery` |
| Java de compilacion del proyecto | 17 (`javac --release 17` via Maven) |
| Java runtime detectado en tests | Java 23 en la maquina local |
| AWS real | No usado |
| Docker Hub real | No usado |
| `main` | No modificado |
| `config-repo` externo | No modificado |

## Alcance de la validacion

La validacion final revisa el estado completo del Caso Practico 7 sin introducir funcionalidad nueva. Se validan:

- reactor Maven y modulos activos;
- tests globales y focalizados;
- fronteras hexagonales;
- ausencia de `.block()` en produccion;
- ausencia de credenciales reales;
- Docker Compose de forma estatica;
- scripts PowerShell;
- documentacion final;
- estado de Docker, LocalStack, S3 y Lambda segun disponibilidad del entorno.

## Estado Git

| Comando | Resultado |
| --- | --- |
| `git status` | Worktree limpio al inicio de la validacion |
| `git diff --check` | OK, sin errores |
| `git rev-parse --short HEAD` | `9dc77f9` |

Despues de esta validacion quedan cambios documentales esperados en:

- `README.md`
- `docs/checklist-entrega-m7.md`
- `docs/reporte-validacion-m7.md`

No se realizaron commits.

## Modulos Maven activos

El `pom.xml` padre contiene los modulos activos de la entrega:

- `novabank-events`
- `eureka-server`
- `config-server`
- `api-gateway`
- `auth-server`
- `cliente-service`
- `cuenta-service`
- `operacion-service`
- `documento-service`
- `comision-lambda`
- `exchange-rate-mock-service`
- `notificacion-service`

No se incluye `novabank-monolith` en el flujo final.

## Resultados Maven

| Comando | Resultado |
| --- | --- |
| `mvn clean test-compile` | OK en todo el reactor |
| `mvn clean test` | Falla en `auth-server` por Testcontainers sin Docker |
| `mvn -pl novabank-events test` | OK; no hay tests |
| `mvn -pl documento-service test` | OK, 15 tests |
| `mvn -pl comision-lambda test` | OK, 11 tests |
| `mvn -pl notificacion-service test` | OK, 4 tests |
| `mvn -pl cuenta-service test -Dtest=OperacionResultadoEventPublisherTest` | OK, 5 tests |
| `mvn -pl operacion-service test -Dtest=OperacionSagaServiceTest,OperacionControllerTest,LambdaComisionCalculatorAdapterTest,OperacionResultadoEventConsumerConfigTest` | OK, 45 tests |
| `mvn -pl comision-lambda package` | OK, jar sombreado generado |

### Fallo conocido de `mvn clean test`

Modulo afectado:

- `auth-server`

Tests afectados:

- `AuthOpenApiSecurityTest`
- `UsuarioRepositoryTest`

Error literal:

```text
Could not find a valid Docker environment
```

Detalle adicional:

```text
Previous attempts to find a Docker environment failed. Will not retry. Please see logs and check configuration
```

Causa:

- Docker Desktop/daemon no esta disponible en el entorno actual.
- Testcontainers no puede arrancar `postgres:16-alpine`.
- El reactor se detiene en `auth-server`; los modulos posteriores quedan `SKIPPED` en esa ejecucion global.

Confirmacion alternativa:

- `mvn clean test-compile` valida compilacion y test-compile de todos los modulos.
- Los tests focalizados sin Docker de los modulos M7 pasan.

## Fronteras hexagonales

Comprobaciones ejecutadas contra `application` y `domain` de:

- `cuenta-service`
- `operacion-service`
- `documento-service`

Patrones buscados:

```text
StreamBridge|Message<|KafkaHeaders|ReactiveCrudRepository|WebClient|software.amazon.awssdk|LambdaAsyncClient|InvokeRequest|S3AsyncClient|S3Presigner
```

Resultado:

- salida vacia en los tres servicios;
- no hay dependencias reales de Kafka Spring, WebClient, R2DBC ni SDK AWS en dominio/aplicacion;
- los adaptadores mantienen la infraestructura fuera del nucleo.

## Busqueda de `.block()`

Comando:

```powershell
Select-String -Path "**/src/main/java/**/*.java" -Pattern "\.block\("
```

Resultado:

- salida vacia;
- no se detectan `.block()` en codigo de produccion.

## Credenciales

Patrones buscados:

```text
AKIA|aws_secret_access_key|SECRET_ACCESS_KEY|BEGIN PRIVATE KEY|password:|Password=
```

Resultado:

- no se detectan credenciales reales;
- aparecen valores dummy `AWS_SECRET_ACCESS_KEY = "test"` para LocalStack;
- aparecen passwords locales de Docker como `novabank`, documentados para entorno local.

## Docker Compose

| Comando | Resultado |
| --- | --- |
| `docker version` | Docker CLI instalado, daemon no accesible |
| `docker compose config` | OK; YAML valido |

Error de Docker daemon:

```text
failed to connect to the docker API at npipe:////./pipe/docker_engine; check if the path is correct and if the daemon is running: open //./pipe/docker_engine: The system cannot find the file specified.
```

Warning observado:

```text
WARNING: Error loading config file: open C:\Users\Usuario\.docker\config.json: Access is denied.
```

Revisiones de Compose:

- los servicios de negocio usan `kafka:9092` dentro de Docker;
- los servicios con persistencia usan `postgres:5432`;
- `operacion-service` y `documento-service` usan `http://localstack:4566`;
- Config Server usa `classpath:/config-repo-docker`;
- no se monta el `config-repo` externo;
- `docker-compose.yml` usa rutas relativas;
- las rutas absolutas Windows aparecen solo en la salida normalizada de `docker compose config`;
- `localhost` en Kafka corresponde al listener externo para clientes desde host;
- `localhost` en el healthcheck de LocalStack es interno al propio contenedor.

## Docker, LocalStack, S3 y Lambda

No se ejecuto:

- `docker compose build`;
- `docker compose up -d`;
- `docker compose ps`;
- comprobaciones `curl` contra Eureka, Config Server, Gateway, Kafka UI o LocalStack;
- `aws --endpoint-url=http://localhost:4566 s3 ls`;
- despliegue real de Lambda en LocalStack;
- invocacion real de Lambda en LocalStack.

Motivo:

- Docker daemon no disponible.
- AWS CLI no aparece en PATH.

Validado como alternativa:

- `mvn -pl comision-lambda package` funciona y genera el artefacto desplegable.

## Scripts

Scripts revisados:

- `scripts/build-docker-images.ps1`
- `scripts/start-local-platform.ps1`
- `scripts/tag-docker-images.ps1`
- `scripts/push-docker-images.ps1`
- `scripts/deploy-comision-lambda-localstack.ps1`
- `scripts/invoke-comision-lambda-localstack.ps1`

Resultado:

- todos parsean correctamente como PowerShell;
- no contienen credenciales reales;
- los scripts LocalStack usan credenciales dummy `test`;
- `push-docker-images.ps1` muestra las imagenes y exige confirmacion `PUSH` salvo uso explicito de `-Yes`;
- no se ejecuto push real a Docker Hub.

## Documentacion revisada

Documentos presentes:

- `docs/modulo-7-cloud-native.md`
- `docs/docker-cloud-native.md`
- `docs/lambda-comisiones.md`
- `docs/kubernetes-eks-teorico.md`
- `docs/aws-deployment-options.md`
- `docs/aws-security-iam-secrets.md`
- `docs/aws-api-gateway.md`
- `docs/aws-cost-model.md`
- `docs/checklist-entrega-m7.md`
- `docs/reporte-validacion-m7.md`

`README.md` enlaza la documentacion principal del Modulo 7 e incluye comandos de tests, Docker, Lambda local, S3, transferencia internacional y entrega final.

## Flujos funcionales

No se ejecutaron flujos end-to-end reales porque requieren Docker Compose, Kafka, PostgreSQL, LocalStack y la Lambda desplegada.

Quedan documentados para ejecutar cuando el entorno este disponible:

- transferencia ordinaria y SAGA completada;
- generacion de justificante tras `OperacionCompletadaEvent`;
- consulta/listado de documentos;
- transferencia internacional con Lambda disponible;
- transferencia internacional con Lambda no disponible.

No se inventan resultados de ejecucion end-to-end.

## Conclusion

El proyecto queda preparado para PR final hacia `develop` desde el punto de vista de compilacion, documentacion, frontera arquitectonica y pruebas focalizadas sin Docker.

El unico bloqueo para una validacion completa de runtime es de entorno:

```text
Could not find a valid Docker environment
```

Antes de merge/tag final se recomienda ejecutar de nuevo, con Docker Desktop activo y AWS CLI disponible:

```powershell
mvn clean test
docker compose build
docker compose up -d
docker compose ps
aws --endpoint-url=http://localhost:4566 s3 ls
mvn -pl comision-lambda package
.\scripts\deploy-comision-lambda-localstack.ps1
.\scripts\invoke-comision-lambda-localstack.ps1
```

No se ha creado tag `v7.0.0`, no se ha mergeado a `main`, no se ha hecho push a Docker Hub y no se ha usado AWS real.
