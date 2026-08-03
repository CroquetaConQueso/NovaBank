# Lambda de comisiones internacionales

## Proposito

`comision-lambda` calcula la comision aplicable a una transferencia internacional. `operacion-service` la invoca para transferencias asincronas marcadas como internacionales, antes de iniciar la SAGA Kafka.

## Por que no usa Spring Boot

La funcion debe arrancar rapido y desplegarse como Lambda Java simple en AWS o LocalStack. Por eso usa `RequestHandler` de `aws-lambda-java-core` y no carga contexto Spring ni dependencias de microservicios.

## Handler

```text
com.novabank.lambda.comision.ComisionHandler::handleRequest
```

## Entrada JSON

```json
{
  "importeEuros": 1000,
  "paisDestino": "US",
  "tipoCliente": "PARTICULAR"
}
```

## Salida JSON

```json
{
  "comision": 20.00,
  "tasaAplicada": 0.0200,
  "paisDestino": "US",
  "tipoCliente": "PARTICULAR"
}
```

## Reglas de comision

| Pais destino | Tasa base |
| --- | ---: |
| `US` | `0.020` |
| `GB` | `0.018` |
| `MX` | `0.030` |
| `MA` | `0.035` |
| desconocido | `0.025` |

Reglas adicionales:

- `EMPRESA` recibe un descuento del 20% sobre la tasa seleccionada.
- Cualquier `tipoCliente` desconocido se trata como `PARTICULAR`.
- `paisDestino` desconocido usa la tasa base por defecto `0.025`.
- La comision final se redondea a 2 decimales con `RoundingMode.HALF_UP`.
- `importeEuros` debe ser mayor que cero.
- `importeEuros`, `paisDestino` y `tipoCliente` son obligatorios.
- `paisDestino` y `tipoCliente` se normalizan con `trim` y mayusculas.
- Las solicitudes invalidas lanzan `InvalidComisionRequestException`.

## Build y tests

```powershell
mvn -pl comision-lambda test
mvn -pl comision-lambda package
```

El JAR desplegable se genera como:

```text
comision-lambda/target/comision-lambda-4.0-SNAPSHOT-aws.jar
```

## Despliegue LocalStack

Script preparado:

```powershell
.\scripts\deploy-comision-lambda-localstack.ps1
```

La funcion se publica como `novabank-comision`. El script usa credenciales ficticias locales y endpoint LocalStack; no requiere ni persiste credenciales AWS reales.

## Invocacion LocalStack

Script preparado:

```powershell
.\scripts\invoke-comision-lambda-localstack.ps1
```

Payload de ejemplo:

```json
{
  "importeEuros": 1000,
  "paisDestino": "US",
  "tipoCliente": "EMPRESA"
}
```

La respuesta se escribe por defecto en `comision-lambda-response.json`.

## Integracion con operacion-service

`operacion-service` usa un puerto de aplicacion (`ComisionCalculatorPort`) y un adaptador de salida (`LambdaComisionCalculatorAdapter`). El dominio y la aplicacion no dependen del SDK de AWS; `LambdaAsyncClient`, `InvokeRequest` y los DTOs JSON quedan dentro de `adapter/out/lambda`.

Configuracion local:

```yaml
novabank:
  aws:
    region: eu-west-1
    endpoint-override: http://localhost:4566
  lambda:
    comision-function-name: novabank-comision
```

Endpoint:

```http
POST /api/operaciones/transferencia
```

Ejemplo de transferencia internacional:

```json
{
  "cuentaOrigenId": 10,
  "cuentaDestinoId": 11,
  "cantidad": 1000.00,
  "internacional": true,
  "paisDestino": "US",
  "tipoCliente": "EMPRESA"
}
```

Si `internacional` es `false` o no se informa, el endpoint mantiene el comportamiento anterior y no invoca Lambda.

Si `internacional` es `true`, `paisDestino` y `tipoCliente` son obligatorios. Si la Lambda no responde, devuelve error o el payload no se puede procesar, `operacion-service` devuelve `503` con codigo `LAMBDA_COMISION_UNAVAILABLE`. En ese caso no se inventa una comision por defecto, no se publica `OperacionSolicitadaEvent` y la SAGA no se inicia.

Antes de probar una transferencia internacional en local:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "eu-west-1"
.\scripts\deploy-comision-lambda-localstack.ps1
mvn -pl operacion-service spring-boot:run
```

`operacion-service` usa `DefaultCredentialsProvider`; las variables anteriores son credenciales ficticias para LocalStack, no credenciales AWS reales.
