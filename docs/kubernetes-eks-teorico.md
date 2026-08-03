# Kubernetes y EKS - enfoque teorico para NovaBank

Este documento explica como llevar NovaBank a Kubernetes/EKS de forma teorica. No implica despliegue real ni creacion de recursos AWS.

## Objetivo

Kubernetes aporta empaquetado, despliegue declarativo, escalado, health checks y aislamiento operativo para los microservicios. EKS ofrece el plano de control gestionado por AWS.

## Mapeo de componentes

| NovaBank local | Kubernetes/EKS |
| --- | --- |
| Contenedor de microservicio | `Deployment` |
| Nombre de servicio Docker Compose | `Service` ClusterIP |
| Variables de entorno comunes | `ConfigMap` |
| Passwords, tokens o claves | `Secret` o AWS Secrets Manager |
| API Gateway HTTP local | `Ingress` + Load Balancer o AWS API Gateway |
| Replicas manuales | `HorizontalPodAutoscaler` |
| Logs de consola | CloudWatch Logs / stack de observabilidad |

## Deployments

Cada microservicio Spring Boot se ejecutaria como `Deployment` independiente. El manifiesto debe definir:

- imagen versionada;
- puerto del contenedor;
- variables de entorno;
- probes de salud;
- limites y requests de CPU/memoria;
- replicas minimas.

Los ejemplos incluidos cubren `api-gateway`, `operacion-service` y `documento-service`. El resto seguiria el mismo patron.

## Services

Cada `Deployment` expone un `Service` interno. Los microservicios pueden comunicarse por DNS de Kubernetes (`http://cuenta-service:8082`) o mantener Eureka si el objetivo docente es conservar Spring Cloud Discovery.

En una migracion real se evaluaria si Eureka sigue aportando valor. Kubernetes ya resuelve service discovery basico mediante DNS y Services.

## ConfigMaps y Secrets

`k8s/configmap.yaml` contiene configuracion no sensible. `k8s/secrets-example.yaml` muestra el patron para secretos, pero no contiene credenciales reales.

En AWS real se recomienda:

- Secrets Manager o SSM Parameter Store para secretos;
- IAM Roles for Service Accounts para permisos AWS;
- rotacion de secretos cuando aplique;
- no empaquetar secretos en imagenes Docker.

## Ingress

El ejemplo `k8s/ingress-example.yaml` enruta el trafico externo hacia `api-gateway`. En EKS se podria usar AWS Load Balancer Controller para crear un ALB.

El Gateway de Spring seguiria centralizando rutas internas, validacion JWT y Swagger agregado. AWS API Gateway tambien podria colocarse delante como borde gestionado; esa alternativa se analiza en [aws-api-gateway.md](aws-api-gateway.md).

## HPA

`k8s/hpa-example.yaml` muestra escalado horizontal por CPU para `api-gateway` y `operacion-service`. En un entorno real se ajustarian metricas segun carga:

- peticiones por segundo;
- latencia;
- lag de Kafka;
- CPU/memoria;
- metricas de negocio.

## Estado y dependencias

PostgreSQL, Kafka y LocalStack no deberian ejecutarse igual que en Compose para produccion. Alternativas gestionadas:

| Dependencia | Alternativa AWS |
| --- | --- |
| PostgreSQL | Amazon RDS/Aurora PostgreSQL |
| Kafka | Amazon MSK |
| S3 LocalStack | Amazon S3 |
| Lambda LocalStack | AWS Lambda |
| Logs locales | CloudWatch |

## Probes

Los manifiestos usan `/actuator/health`. Para produccion conviene activar grupos de health:

- readiness: el servicio puede recibir trafico;
- liveness: el proceso sigue sano;
- startup: tolera arranques lentos de Spring Boot.

## Consideraciones previas a un despliegue real

- Definir namespace por entorno.
- Publicar imagenes en un registry confiable.
- Sustituir placeholders de Docker Hub.
- Configurar TLS y dominio real.
- Configurar secrets fuera de Git.
- Revisar politicas IAM.
- Definir limites de recursos.
- Configurar observabilidad y alertas.
- Ejecutar pruebas de resiliencia y rollback.
