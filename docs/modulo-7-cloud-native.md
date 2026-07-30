# Modulo 7 - Cloud native, S3, Lambda y entrega Docker Hub

Este documento resume el estado del Caso Practico 7 de NovaBank. El objetivo es demostrar una evolucion cloud native local sin desplegar en AWS real: Docker Compose levanta la plataforma, LocalStack simula S3 y Lambda, y los artefactos quedan preparados para una entrega documentada y reproducible.

## Alcance funcional

- `documento-service` gestiona justificantes de operaciones con arquitectura hexagonal.
- Los justificantes se almacenan como JSON en un bucket S3 compatible con LocalStack.
- `operacion-service` invoca una Lambda local para calcular comisiones de transferencias internacionales.
- El flujo Kafka mantiene los topics y bindings definidos en modulos anteriores.
- El API Gateway sigue siendo la entrada HTTP principal y agrega OpenAPI.
- La plataforma local se ejecuta mediante Docker Compose.

No se introducen credenciales reales, despliegues reales en AWS ni cambios en contratos publicos.

## Servicios del modulo

| Componente | Responsabilidad | Puerto local |
| --- | --- | ---: |
| `api-gateway` | Entrada HTTP, seguridad perimetral y Swagger agregado | 8080 |
| `auth-server` | Registro, login y validacion JWT | 9000 |
| `cliente-service` | Clientes | 8081 |
| `cuenta-service` | Cuentas, saldos, consumidor Kafka y SSE | 8082 |
| `operacion-service` | Operaciones, SAGA y comision Lambda | 8083 |
| `exchange-rate-mock-service` | Tipo de cambio mock | 8084 |
| `notificacion-service` | Consumidores Kafka sin API REST publica | 8085 interno |
| `documento-service` | Justificantes y URL de descarga | 8086 |
| `eureka-server` | Service discovery | 8761 |
| `config-server` | Configuracion centralizada | 8888 |
| Kafka | Broker de eventos | 9094 host / 9092 interno |
| Kafka UI | Observabilidad de topics | 8090 |
| PostgreSQL | Persistencia local | 5432 |
| LocalStack | S3 y Lambda locales | 4566 |

## Flujos principales

### Operacion bancaria

1. El cliente invoca `operacion-service` a traves de `api-gateway`.
2. `operacion-service` valida la peticion, aplica idempotencia y publica `OperacionSolicitadaEvent`.
3. `cuenta-service` consume la solicitud, actualiza saldos y publica:
   - `OperacionCompletadaEvent`, o
   - `OperacionFallidaEvent`.
4. Cuando procede, `cuenta-service` publica `MovimientoRegistradoEvent`.
5. El bus SSE de cuenta mantiene el streaming de movimientos.
6. `documento-service` consume operaciones completadas para generar justificantes.

### Justificante S3

1. `documento-service` recibe un evento de operacion completada.
2. El caso de uso de aplicacion prepara el documento.
3. El puerto de storage persiste el JSON en S3 compatible con LocalStack.
4. El endpoint HTTP permite listar documentos por cuenta, eliminar por operacion y generar URL de descarga.

Endpoints:

| Metodo | Ruta |
| --- | --- |
| `GET` | `/api/documentos/operaciones/{operacionId}/url` |
| `GET` | `/api/documentos/cuentas/{cuentaId}` |
| `DELETE` | `/api/documentos/operaciones/{operacionId}` |

### Comision Lambda

1. `operacion-service` detecta una transferencia internacional.
2. El caso de uso delega el calculo de comision en `ComisionCalculatorPort`.
3. El adaptador Lambda invoca la funcion `novabank-comision` en LocalStack.
4. Si Lambda no responde o devuelve error, la operacion falla de forma controlada.

## Infraestructura local

La ejecucion local se documenta en [docker-cloud-native.md](docker-cloud-native.md). Comandos base:

```powershell
docker compose build
docker compose up -d
```

Scripts auxiliares:

```powershell
.\scripts\build-docker-images.ps1
.\scripts\start-local-platform.ps1 -Build
```

Despliegue manual de Lambda local:

```powershell
mvn -pl comision-lambda package
.\scripts\deploy-comision-lambda-localstack.ps1
```

## Docker Hub

La rama incluye scripts seguros para etiquetar y publicar imagenes, pero no ejecutan login ni push sin accion explicita del operador.

Preparar tags:

```powershell
$env:DOCKERHUB_USER = "usuario-dockerhub"
$env:VERSION = "7.0.0"
.\scripts\tag-docker-images.ps1
```

Publicar:

```powershell
docker login
.\scripts\push-docker-images.ps1
```

Para publicar tambien `latest`:

```powershell
.\scripts\tag-docker-images.ps1 -Latest
.\scripts\push-docker-images.ps1 -Latest
```

## Kubernetes y AWS

La carpeta `k8s/` contiene manifiestos de ejemplo para explicar una migracion teorica a EKS:

- Deployments y Services para `api-gateway`, `operacion-service` y `documento-service`.
- ConfigMap comun.
- Secret de ejemplo sin valores reales.
- Ingress de ejemplo.
- HPA de ejemplo.

La comparativa de opciones AWS, seguridad, API Gateway y costes esta separada en:

- [kubernetes-eks-teorico.md](kubernetes-eks-teorico.md)
- [aws-deployment-options.md](aws-deployment-options.md)
- [aws-security-iam-secrets.md](aws-security-iam-secrets.md)
- [aws-api-gateway.md](aws-api-gateway.md)
- [aws-cost-model.md](aws-cost-model.md)

## Limitaciones asumidas

- No hay despliegue real en AWS.
- No se incluyen credenciales reales.
- LocalStack simula S3 y Lambda, no reemplaza una validacion productiva completa.
- Los tests con Testcontainers requieren Docker Desktop disponible.
- Los manifiestos Kubernetes son base teorica y necesitan ajuste de imagenes, dominios, TLS, recursos y secretos antes de usarse en un cluster real.

## Validacion recomendada

```powershell
git diff --check
mvn -pl comision-lambda test
mvn -pl documento-service test
mvn -pl operacion-service test
docker compose config
docker compose build
```

Si Docker Desktop no esta disponible, `docker compose build` y los tests Testcontainers pueden fallar por entorno, no por cambios funcionales.
