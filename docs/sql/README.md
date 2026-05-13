# Scripts SQL - NovaBank Modulo 5

Estos scripts preparan las bases PostgreSQL necesarias para ejecutar NovaBank en local con servicios reactivos y Spring Data R2DBC.

## Script Recomendado

Desde la raiz del proyecto:

```powershell
psql -U postgres -f docs/sql/00-novabank-modulo5-completo.sql
```

`00-novabank-modulo5-completo.sql` es autocontenido:

- se conecta a `postgres`;
- crea las bases si no existen;
- se conecta a cada base;
- crea tablas, constraints e indices;
- inserta la fila inicial de `account_number_sequence`;
- incluye tablas de idempotencia interna y publica.

## Scripts Individuales

Tambien se pueden ejecutar los scripts por servicio:

```powershell
psql -U postgres -f docs/sql/01-novabank-auth.sql
psql -U postgres -f docs/sql/02-novabank-clientes.sql
psql -U postgres -f docs/sql/03-novabank-cuentas.sql
psql -U postgres -f docs/sql/04-novabank-operaciones.sql
```

## Bases De Datos

| Script | Base de datos | Servicio | Tablas |
| --- | --- | --- | --- |
| `01-novabank-auth.sql` | `novabank_auth` | `auth-server` | `usuarios` |
| `02-novabank-clientes.sql` | `novabank_clientes` | `cliente-service` | `clientes` |
| `03-novabank-cuentas.sql` | `novabank_cuentas` | `cuenta-service` | `cuentas`, `account_number_sequence`, `operaciones_idempotentes` |
| `04-novabank-operaciones.sql` | `novabank_operaciones` | `operacion-service` | `movimientos`, `operaciones_publicas_idempotentes` |

## Relacion Con R2DBC

Los servicios usan Spring Data R2DBC. Los scripts crean el esquema esperado por los modelos reactivos del proyecto, sin depender de variables de entorno ni incluir contrasenas.

Config Server debe apuntar cada servicio a su base correspondiente mediante propiedades `spring.r2dbc.*`.

## Comprobaciones Utiles

Listar bases:

```sql
\l
```

Entrar en la base de cuentas:

```sql
\c novabank_cuentas
```

Listar tablas:

```sql
\dt
```

Comprobar la secuencia funcional de numeros de cuenta:

```sql
SELECT * FROM account_number_sequence;
```

Comprobar tablas de idempotencia:

```sql
\c novabank_cuentas
SELECT operation_id, estado FROM operaciones_idempotentes;

\c novabank_operaciones
SELECT idempotency_key, tipo_operacion, estado FROM operaciones_publicas_idempotentes;
```

## Si Falta Una Tabla

1. Confirmar que se ejecuto el script completo o el script individual del servicio afectado.
2. Confirmar que el YAML externo entregado por Config Server apunta a la base correcta.
3. Entrar en la base con `psql` y ejecutar `\dt`.
4. Reejecutar el script correspondiente si la tabla no existe.

Ejemplo:

```powershell
psql -U postgres -f docs/sql/00-novabank-modulo5-completo.sql
```

## Si Config Server Arranca Pero Un Servicio Falla Por Esquema

Config Server solo entrega propiedades. Si un servicio falla por esquema, revisar:

- que la base configurada existe;
- que el usuario de PostgreSQL tiene permisos sobre la base;
- que el script se ejecuto contra la base correcta;
- que no queda una tabla antigua con estructura incompatible.

## Reset Limpio Opcional

Los scripts no destruyen datos. Usan `CREATE DATABASE` y `CREATE TABLE IF NOT EXISTS`.

Si una base quedo con una estructura incompatible, guardar primero los datos necesarios y recrear solo la base afectada:

```powershell
dropdb -U postgres novabank_cuentas
psql -U postgres -f docs/sql/03-novabank-cuentas.sql
```

Para recrear todo el entorno local:

```powershell
dropdb -U postgres novabank_auth
dropdb -U postgres novabank_clientes
dropdb -U postgres novabank_cuentas
dropdb -U postgres novabank_operaciones
psql -U postgres -f docs/sql/00-novabank-modulo5-completo.sql
```
