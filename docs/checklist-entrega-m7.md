# Checklist de entrega - Modulo 7

Estado actualizado en la rama `feature/m7-final-stabilization`.

Leyenda:

| Estado | Significado |
| --- | --- |
| Validado | Comprobado en esta iteracion. |
| Pendiente por entorno | Requiere Docker Desktop, LocalStack, AWS CLI u otro componente no disponible. |
| No aplicado | No debe ejecutarse desde Codex o no forma parte de esta validacion. |

## Git y alcance

| Comprobacion | Estado | Evidencia |
| --- | --- | --- |
| Rama correcta | Validado | `feature/m7-final-stabilization` |
| Worktree inicial limpio | Validado | `git status` sin cambios antes de documentar la validacion |
| No se modifica `main` | Validado | No se hizo checkout, merge ni push a `main` |
| No se toca `config-repo` externo | Validado | Solo se trabajo en el repositorio NovaBank |
| `git diff --check` | Validado | Sin errores |
| Sin credenciales reales | Validado | Solo aparecen credenciales dummy `test` y passwords locales Docker |
| Sin `.block()` nuevo en produccion | Validado | Busqueda sin resultados en `src/main/java` |

## Maven

| Comando | Estado | Resultado |
| --- | --- | --- |
| `mvn clean test-compile` | Validado | Reactor completo OK |
| `mvn clean test` | Pendiente por entorno | Falla en `auth-server` por Testcontainers sin Docker: `Could not find a valid Docker environment` |
| `mvn -pl novabank-events test` | Validado | Build OK; no hay tests |
| `mvn -pl documento-service test` | Validado | 15 tests OK |
| `mvn -pl comision-lambda test` | Validado | 11 tests OK |
| `mvn -pl notificacion-service test` | Validado | 4 tests OK |
| `mvn -pl cuenta-service test -Dtest=OperacionResultadoEventPublisherTest` | Validado | 5 tests OK |
| `mvn -pl operacion-service test -Dtest=OperacionSagaServiceTest,OperacionControllerTest,LambdaComisionCalculatorAdapterTest,OperacionResultadoEventConsumerConfigTest` | Validado | 45 tests OK |
| `mvn -pl comision-lambda package` | Validado | Jar sombreado generado correctamente |

## Fronteras hexagonales

| Servicio | Estado | Resultado |
| --- | --- | --- |
| `cuenta-service` application/domain | Validado | Sin dependencias a Kafka Spring, WebClient, R2DBC, AWS SDK |
| `operacion-service` application/domain | Validado | Sin dependencias a Kafka Spring, WebClient, R2DBC, AWS SDK real |
| `documento-service` application/domain | Validado | Sin dependencias a Kafka Spring, WebClient, R2DBC, S3/Lambda SDK |

## Docker Compose

| Comprobacion | Estado | Resultado |
| --- | --- | --- |
| `docker version` | Pendiente por entorno | CLI instalado, daemon no accesible: `failed to connect to the docker API at npipe:////./pipe/docker_engine` |
| `docker compose config` | Validado | YAML valido; warning por `C:\Users\Usuario\.docker\config.json: Access is denied` |
| `docker compose build` | Pendiente por entorno | No ejecutado porque Docker daemon no esta disponible |
| `docker compose up -d` | Pendiente por entorno | No ejecutado porque Docker daemon no esta disponible |
| `docker compose ps` | Pendiente por entorno | No ejecutado porque no se levanto la plataforma |
| Nombres internos Docker | Validado | Servicios usan `kafka:9092`, `postgres:5432`, `localstack:4566`, `config-server:8888`, `eureka-server:8761` |
| Rutas absolutas Windows en YAML fuente | Validado | `docker-compose.yml` usa rutas relativas; Compose normaliza a rutas absolutas al imprimir config |

## Infraestructura local

| Comprobacion | Estado | Resultado |
| --- | --- | --- |
| Eureka `http://localhost:8761` | Pendiente por entorno | Requiere `docker compose up` |
| Config Server `http://localhost:8888` | Pendiente por entorno | Requiere `docker compose up` |
| API Gateway `http://localhost:8080` | Pendiente por entorno | Requiere `docker compose up` |
| Kafka UI `http://localhost:8090` | Pendiente por entorno | Requiere `docker compose up` |
| LocalStack `http://localhost:4566` | Pendiente por entorno | Requiere `docker compose up` |

## S3 y Lambda LocalStack

| Comprobacion | Estado | Resultado |
| --- | --- | --- |
| AWS CLI disponible | Pendiente por entorno | `aws` no aparece en PATH |
| Bucket `novabank-justificantes` | Pendiente por entorno | Requiere LocalStack en ejecucion |
| Despliegue Lambda LocalStack | Pendiente por entorno | Requiere Docker/LocalStack y AWS CLI |
| Invocacion Lambda LocalStack | Pendiente por entorno | Requiere Docker/LocalStack y AWS CLI |
| Compilacion Lambda | Validado | `mvn -pl comision-lambda package` OK |

## Flujos funcionales

| Flujo | Estado | Resultado |
| --- | --- | --- |
| Transferencia ordinaria completa SAGA | Pendiente por entorno | Requiere plataforma Docker levantada |
| Generacion de justificante tras operacion completada | Pendiente por entorno | Requiere Kafka + LocalStack S3 |
| URL prefirmada de justificante | Pendiente por entorno | Requiere LocalStack S3 |
| Transferencia internacional con Lambda disponible | Pendiente por entorno | Requiere Lambda desplegada en LocalStack |
| Transferencia internacional con Lambda no disponible | Pendiente por entorno | Requiere plataforma Docker levantada |

## Docker Hub

| Comprobacion | Estado | Resultado |
| --- | --- | --- |
| Scripts de build/start parsean en PowerShell | Validado | Sintaxis OK |
| Scripts de tag/push parsean en PowerShell | Validado | Sintaxis OK |
| `push-docker-images.ps1` exige confirmacion | Validado | Requiere escribir `PUSH` o usar `-Yes` explicitamente |
| `docker login` | No aplicado | Debe ejecutarlo el operador |
| Push real a Docker Hub | No aplicado | Prohibido sin confirmacion explicita |

## Documentacion

| Documento | Estado |
| --- | --- |
| `README.md` | Validado |
| `docs/modulo-7-cloud-native.md` | Validado |
| `docs/docker-cloud-native.md` | Validado |
| `docs/lambda-comisiones.md` | Validado |
| `docs/kubernetes-eks-teorico.md` | Validado |
| `docs/aws-deployment-options.md` | Validado |
| `docs/aws-security-iam-secrets.md` | Validado |
| `docs/aws-api-gateway.md` | Validado |
| `docs/aws-cost-model.md` | Validado |
| `docs/checklist-entrega-m7.md` | Validado |
| `docs/reporte-validacion-m7.md` | Validado |

## Comandos finales sugeridos

No ejecutar estos comandos hasta aprobar la PR final y confirmar la estrategia de ramas:

```powershell
git checkout develop
git pull origin develop
git checkout main
git pull origin main
git merge develop
git tag v7.0.0
git push origin main
git push origin v7.0.0
```
