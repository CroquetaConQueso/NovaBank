# Checklist de entrega - Modulo 7

## Git

- [ ] Rama correcta: `feature/m7-dockerhub-docs-delivery`.
- [ ] No se modifica `main`.
- [ ] No se toca `config-repo` externo.
- [ ] `git diff --check` sin errores.
- [ ] Cambios revisados antes de commit.

## Maven

- [ ] `mvn -pl comision-lambda test`.
- [ ] `mvn -pl documento-service test`.
- [ ] `mvn -pl operacion-service test`.
- [ ] Si `operacion-service` falla por Testcontainers, documentar error exacto de Docker.

## Docker Compose

- [ ] Docker Desktop disponible.
- [ ] `docker compose config`.
- [ ] `docker compose build`.
- [ ] `docker compose up -d` si el entorno lo permite.
- [ ] `docker compose ps`.
- [ ] `docker compose down` al terminar.

## Infraestructura local

- [ ] Eureka disponible en `http://localhost:8761`.
- [ ] API Gateway disponible en `http://localhost:8080`.
- [ ] Swagger agregado disponible en `http://localhost:8080/swagger-ui/index.html`.
- [ ] Kafka UI disponible en `http://localhost:8090`.
- [ ] LocalStack disponible en `http://localhost:4566`.
- [ ] Bucket `novabank-justificantes` creado.

## Kafka

- [ ] `novabank.operaciones.solicitadas`.
- [ ] `novabank.operaciones.completadas`.
- [ ] `novabank.operaciones.fallidas`.
- [ ] `novabank.movimientos.registrados`.
- [ ] `novabank.alertas.saldo-bajo`.
- [ ] `novabank.operaciones.completadas` consumido por `documento-service`.

## Flujo de negocio

- [ ] Deposito valido termina en evento completado.
- [ ] Retirada valida termina en evento completado.
- [ ] Retirada sin saldo termina en evento fallido.
- [ ] Transferencia valida termina en evento completado.
- [ ] Transferencia invalida termina en evento fallido.
- [ ] Transferencia internacional invoca Lambda de comision.
- [ ] Error de Lambda produce fallo controlado.
- [ ] Movimiento registrado sigue publicandose.
- [ ] Alerta de saldo bajo sigue publicandose cuando corresponde.
- [ ] SSE no se rompe.

## Documento-service

- [ ] Justificante generado tras operacion completada.
- [ ] Justificante almacenado en S3 compatible con LocalStack.
- [ ] `GET /api/documentos/operaciones/{operacionId}/url`.
- [ ] `GET /api/documentos/cuentas/{cuentaId}`.
- [ ] `DELETE /api/documentos/operaciones/{operacionId}`.

## Docker Hub

- [ ] `docker login` ejecutado manualmente por el operador.
- [ ] `DOCKERHUB_USER` definido.
- [ ] `VERSION` definido.
- [ ] `.\scripts\tag-docker-images.ps1`.
- [ ] `.\scripts\push-docker-images.ps1`.
- [ ] No se publican imagenes sin confirmacion explicita.

## Documentacion

- [ ] README actualizado.
- [ ] `docs/modulo-7-cloud-native.md`.
- [ ] `docs/docker-cloud-native.md`.
- [ ] `docs/kubernetes-eks-teorico.md`.
- [ ] `docs/aws-deployment-options.md`.
- [ ] `docs/aws-security-iam-secrets.md`.
- [ ] `docs/aws-api-gateway.md`.
- [ ] `docs/aws-cost-model.md`.
