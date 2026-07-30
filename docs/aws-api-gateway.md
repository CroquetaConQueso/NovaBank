# AWS API Gateway y Spring Cloud Gateway

NovaBank usa Spring Cloud Gateway como entrada HTTP local. AWS API Gateway puede complementar o sustituir parte de ese borde en un despliegue real.

## Spring Cloud Gateway actual

Responsabilidades actuales:

- enrutar a microservicios por nombre logico;
- validar JWT en el borde;
- propagar trazas;
- exponer Swagger/OpenAPI agregado;
- mantener una experiencia local uniforme con Docker Compose.

Ventajas:

- se versiona con la aplicacion;
- es facil de ejecutar localmente;
- mantiene integracion directa con Spring Cloud;
- permite filtros personalizados en Java.

## AWS API Gateway

Responsabilidades posibles:

- publicar APIs en internet;
- throttling y cuotas;
- authorizers;
- integracion con WAF;
- metricas y logs gestionados;
- versionado/stages;
- entrada hacia Lambda, ALB, VPC Link o servicios privados.

## Patron recomendado

Para NovaBank, una opcion realista es:

```text
Cliente -> AWS API Gateway / ALB -> api-gateway Spring -> microservicios privados
```

Asi se conserva la logica existente de Spring Cloud Gateway y se anade borde gestionado de AWS.

## Alternativas

| Alternativa | Uso recomendado |
| --- | --- |
| Solo Spring Cloud Gateway | Desarrollo local, despliegue simple, control en codigo |
| AWS API Gateway delante de Spring Gateway | Produccion con control gestionado de borde |
| AWS API Gateway directo a servicios | APIs pequenas, rutas simples, menos logica en gateway |
| ALB delante de Spring Gateway | Contenedores privados con balanceo HTTP clasico |

## Seguridad

AWS API Gateway podria aportar:

- rate limiting;
- WAF;
- certificados gestionados;
- authorizers JWT;
- logs de acceso;
- proteccion de borde.

Spring Cloud Gateway seguiria validando requisitos internos y propagando contexto.

## Decision para el modulo

El modulo mantiene Spring Cloud Gateway porque:

- es reproducible localmente;
- no requiere AWS real;
- no cambia contratos publicos;
- permite Swagger agregado en el mismo punto de entrada;
- mantiene continuidad con los modulos anteriores.
