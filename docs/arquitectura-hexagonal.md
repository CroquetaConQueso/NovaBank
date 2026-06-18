# Migracion gradual a arquitectura hexagonal

## Objetivo

La migracion debe realizarse por casos de uso completos, sin reorganizar paquetes de forma masiva ni dejar adaptadores llamando directamente a detalles de infraestructura. Los contratos HTTP, eventos Kafka y esquemas persistidos se mantienen durante cada paso.

## Paquetes objetivo por servicio

```text
com.novabank.<servicio>
|-- domain
|   |-- model
|   `-- service
|-- application
|   |-- port
|   |   |-- in
|   |   `-- out
|   `-- usecase
|-- adapter
|   |-- in
|   |   |-- web
|   |   `-- kafka
|   `-- out
|       |-- persistence
|       |-- kafka
|       `-- http
`-- config
```

## Mapeo del estado actual

| Paquete actual | Destino |
| --- | --- |
| `model` | `domain/model` |
| `service` con reglas puras | `domain/service` |
| `service` que orquesta dependencias | `application/usecase` |
| interfaces de entrada | `application/port/in` |
| repositorios, publishers y clientes como interfaces | `application/port/out` |
| `controller` | `adapter/in/web` |
| consumidores Kafka | `adapter/in/kafka` |
| repositorios R2DBC | `adapter/out/persistence` |
| publishers Kafka | `adapter/out/kafka` |
| clientes WebClient | `adapter/out/http` |
| configuracion Spring | `config` |

## Primer corte recomendado

El primer corte debe ser el procesamiento de `OperacionSolicitadaEvent` en `cuenta-service`:

1. Extraer un puerto de entrada `ProcesarOperacionSolicitadaUseCase`.
2. Modelar el resultado de aplicacion como completado o rechazado, sin incluir clases de Kafka.
3. Definir puertos de salida para aplicar movimientos y publicar resultados.
4. Mantener el consumidor Kafka como adaptador de entrada y `StreamBridge` como adaptador de salida.
5. Conservar nombres de bindings, topics y payloads.

Este cambio no se aplica en esta estabilizacion porque exige mover dependencias de `CuentaService` y separar transaccion, eventos de movimiento y alertas. Hacer solo el cambio de paquetes produciria una arquitectura nominal pero incompleta.

## Reglas para siguientes iteraciones

- Migrar un caso de uso vertical por pull request.
- El dominio no depende de Spring, WebFlux, R2DBC, Kafka ni DTOs HTTP.
- Los adaptadores convierten sus payloads a modelos o comandos de aplicacion.
- Los puertos de salida no exponen `StreamBridge`, `WebClient` ni repositorios Spring Data.
- Se permiten `Mono` y `Flux` en puertos de aplicacion mientras el proyecto mantenga su modelo reactivo.
- `documento-service` del Modulo 7 debe nacer con esta estructura, con S3 y Lambda bajo `adapter/out`.
