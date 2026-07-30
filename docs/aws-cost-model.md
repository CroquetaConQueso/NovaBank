# Modelo de costes AWS para NovaBank

Este documento identifica fuentes de coste en una migracion AWS. No incluye precios concretos porque dependen de region, uso, retencion, trafico y configuracion.

## Principales fuentes de coste

| Area | Fuente de coste |
| --- | --- |
| Contenedores | EKS/ECS, CPU, memoria, nodos o Fargate |
| Kubernetes | Plano de control EKS, nodos worker, almacenamiento |
| Red | Load balancers, NAT Gateway, transferencia de datos |
| Base de datos | RDS/Aurora, almacenamiento, backups, replicas |
| Kafka | MSK brokers, almacenamiento, transferencia |
| S3 | almacenamiento, requests, transferencia |
| Lambda | invocaciones, duracion, memoria |
| Logs | ingestion y retencion en CloudWatch |
| Secretos | Secrets Manager y rotacion |
| Observabilidad | metricas, trazas, dashboards y alertas |

## LocalStack y entorno local

El entorno local evita costes de AWS:

- S3 se simula en LocalStack;
- Lambda se simula en LocalStack;
- Kafka, PostgreSQL y servicios corren en Docker local;
- no se crean recursos reales.

## Riesgos de coste

- NAT Gateway activo sin necesidad.
- Logs de alta cardinalidad o retencion excesiva.
- Buckets S3 sin lifecycle.
- Clusters EKS sobredimensionados.
- MSK con brokers permanentes para cargas pequenas.
- Transferencia entre AZs o salida a internet.
- Entornos de prueba no apagados.

## Medidas de control

- Usar presupuestos y alertas de AWS Budgets.
- Etiquetar recursos por proyecto, modulo y entorno.
- Definir lifecycle en S3.
- Ajustar retencion de CloudWatch.
- Usar autoscaling con limites.
- Separar entornos demo de produccion.
- Apagar recursos efimeros.
- Revisar costes antes de habilitar NAT Gateway o MSK.

## Eleccion de plataforma y coste relativo

| Opcion | Coste relativo | Comentario |
| --- | --- | --- |
| Docker local + LocalStack | Muy bajo | Sin AWS real |
| EC2 unica + Docker Compose | Bajo/medio | Simple pero menos robusto |
| ECS Fargate | Medio | Buen equilibrio para contenedores |
| EKS | Medio/alto | Mayor coste operativo y de plataforma |
| Lambda puntual | Bajo para uso esporadico | Ideal para calculos aislados |
| MSK | Medio/alto | Kafka gestionado, coste permanente |

## Checklist antes de AWS real

- [ ] Cuenta AWS con presupuestos configurados.
- [ ] Region seleccionada.
- [ ] Tags obligatorios definidos.
- [ ] Sin credenciales reales en Git.
- [ ] Limites de logs y retencion definidos.
- [ ] Plan de apagado para entornos demo.
- [ ] Revisión de NAT, Load Balancers y MSK.
