# Opciones de despliegue AWS para NovaBank

NovaBank puede desplegarse de varias formas en AWS. Esta comparativa es teorica y no crea recursos.

| Opcion | Encaje | Ventajas | Coste operativo | Limitaciones |
| --- | --- | --- | --- | --- |
| EKS | Microservicios con Kubernetes | Control granular, HPA, ecosistema K8s, patron cloud native completo | Alto | Mayor complejidad inicial y operativa |
| ECS Fargate | Contenedores sin gestionar nodos | Menos operacion que EKS, integracion AWS simple | Medio | Menos portable que Kubernetes |
| App Runner | Servicios HTTP simples | Muy rapido para APIs stateless | Bajo/medio | Menor control de red, discovery y topologias complejas |
| EC2 + Docker Compose | Demo o entorno barato controlado | Simple conceptualmente | Medio | No es una arquitectura cloud native robusta |
| Lambda | Logica event-driven o calculos aislados | Pago por uso, escalado automatico | Bajo para uso esporadico | No encaja para todos los microservicios WebFlux |
| API Gateway + servicios privados | Borde gestionado | Auth, throttling, cuotas, observabilidad | Medio | Requiere diseno de rutas y red |

## Recomendacion docente

Para demostrar los conceptos del modulo:

1. Docker Compose para desarrollo local reproducible.
2. LocalStack para S3 y Lambda sin coste.
3. Manifiestos Kubernetes como preparacion teorica.
4. EKS como opcion cloud native completa.

## Recomendacion pragmatica para produccion inicial

Si el objetivo fuera producir con menor complejidad, ECS Fargate mas servicios gestionados puede ser una opcion mas directa:

- ECS Fargate para microservicios;
- RDS PostgreSQL;
- MSK o Amazon EventBridge segun requisitos de Kafka;
- S3 real para justificantes;
- Lambda real para comisiones;
- AWS Secrets Manager;
- ALB o API Gateway delante.

## Cuando elegir EKS

EKS tiene sentido si el equipo necesita:

- estandar Kubernetes;
- portabilidad multi-cloud;
- operators;
- politicas de red avanzadas;
- escalado fino;
- plataforma interna compartida.

No es la opcion mas simple si el equipo solo necesita ejecutar APIs con pocas dependencias.

## Cuando elegir Lambda

Lambda encaja bien en NovaBank para calculos aislados como comisiones internacionales. No es la forma natural de ejecutar todos los microservicios reactivos porque hay estado de conexion, discovery, streaming SSE y dependencias de larga vida.

## Servicios gestionados recomendables

| Necesidad | Servicio AWS |
| --- | --- |
| Imagenes Docker | ECR o Docker Hub externo |
| Base de datos PostgreSQL | RDS/Aurora |
| Objetos justificantes | S3 |
| Calculo comisiones | Lambda |
| Secretos | Secrets Manager / SSM Parameter Store |
| Logs y metricas | CloudWatch |
| Trafico HTTP externo | ALB / API Gateway |
| Eventos Kafka | MSK |
