# Lambda de comisiones internacionales

## Proposito

`comision-lambda` calcula la comision aplicable a una transferencia internacional. En esta rama solo se crea el artefacto Lambda independiente; la integracion con `operacion-service` queda para M7-06.

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
