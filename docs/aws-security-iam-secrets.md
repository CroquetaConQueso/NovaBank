# Seguridad AWS, IAM y secretos

Este documento define criterios de seguridad para una posible migracion AWS de NovaBank. No incluye credenciales reales.

## Principios

- No versionar claves AWS.
- No empaquetar secretos en imagenes Docker.
- Usar permisos minimos.
- Separar configuracion no sensible de secretos.
- Usar roles IAM en ejecucion, no access keys fijas.
- Registrar auditoria con CloudTrail y logs de aplicacion.

## Entorno local

LocalStack usa credenciales dummy:

```text
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_DEFAULT_REGION=eu-west-1
```

Estos valores no dan acceso a AWS real. Sirven solo para que SDKs y CLI acepten la configuracion local.

## Secretos

Opciones recomendadas:

| Entorno | Mecanismo |
| --- | --- |
| Local | `.env` no versionado o variables de entorno |
| Docker Compose | `env_file` o variables locales |
| Kubernetes demo | `Secret` de ejemplo, sin valores reales |
| AWS real | Secrets Manager o SSM Parameter Store |

`k8s/secrets-example.yaml` es una plantilla. No debe usarse tal cual en produccion.

## IAM por responsabilidad

### `documento-service`

Permisos minimos para S3:

- `s3:PutObject` sobre el prefijo de justificantes;
- `s3:GetObject` para generar/validar descargas;
- `s3:DeleteObject` si se permite borrado;
- `s3:ListBucket` limitado al bucket y prefijos necesarios.

### `operacion-service`

Permisos minimos para Lambda:

- `lambda:InvokeFunction` solo sobre la funcion de comisiones.

### Lambda de comisiones

Permisos minimos:

- escribir logs en CloudWatch;
- leer secretos/configuracion solo si la funcion los necesita.

## Kubernetes en AWS

En EKS se recomienda IAM Roles for Service Accounts (IRSA):

- un service account por microservicio con acceso AWS;
- politicas IAM separadas;
- sin credenciales AWS en variables de entorno;
- tokens de service account gestionados por Kubernetes.

## JWT y claves de aplicacion

El secreto JWT debe:

- almacenarse fuera de Git;
- inyectarse como secreto;
- rotarse con procedimiento controlado;
- tener longitud suficiente;
- no aparecer en logs.

## Checklist de seguridad

- [ ] `.env` no esta versionado.
- [ ] No hay access keys AWS reales en el repositorio.
- [ ] Los manifiestos Kubernetes usan placeholders.
- [ ] Docker Hub no recibe secretos en build args.
- [ ] Los servicios AWS tienen politicas IAM por minimo privilegio.
- [ ] Los buckets S3 bloquean acceso publico salvo justificacion explicita.
- [ ] CloudWatch/observabilidad no registra tokens ni payloads sensibles.
