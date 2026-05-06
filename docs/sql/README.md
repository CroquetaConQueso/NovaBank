# Scripts SQL - NovaBank Modulo 4

Estos scripts preparan las bases PostgreSQL necesarias para ejecutar los microservicios del Modulo 4 en local.

Cada script es autocontenido:

- se conecta a `postgres`;
- crea la base de datos si no existe;
- se conecta a la base correspondiente;
- crea las tablas, indices y constraints que espera Hibernate con `ddl-auto: validate`.

Los scripts no contienen contrasenas y no dependen de variables de entorno.

## Orden Recomendado

Ejecutar desde la raiz del proyecto:

```powershell
psql -U postgres -f docs/sql/01-novabank-auth.sql
psql -U postgres -f docs/sql/02-novabank-clientes.sql
psql -U postgres -f docs/sql/03-novabank-cuentas.sql
psql -U postgres -f docs/sql/04-novabank-operaciones.sql
```

## Bases De Datos

| Script | Base de datos | Tablas |
| --- | --- | --- |
| `01-novabank-auth.sql` | `novabank_auth` | `usuarios` |
| `02-novabank-clientes.sql` | `novabank_clientes` | `clientes` |
| `03-novabank-cuentas.sql` | `novabank_cuentas` | `cuentas`, `account_number_sequence` |
| `04-novabank-operaciones.sql` | `novabank_operaciones` | `movimientos` |

## Relacion Con JPA

Los servicios usan:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

`validate` obliga a que las tablas ya existan y coincidan con las entidades JPA. Si falta una tabla o columna, el servicio falla al arrancar. Ese fallo es intencionado porque evita que Hibernate modifique el esquema de forma silenciosa.

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

## Si Hibernate Indica Missing Table

1. Confirmar que se ejecuto el script del servicio afectado.
2. Confirmar que el YAML del repositorio externo de configuracion apunta a la base correcta.
3. Entrar en la base con `psql` y ejecutar `\dt`.
4. Reejecutar el script correspondiente si la tabla no existe.

Ejemplo:

```powershell
psql -U postgres -f docs/sql/03-novabank-cuentas.sql
```

## Si Config Server Arranca Pero Un Servicio Falla Por Esquema

Config Server solo entrega propiedades. Si un servicio falla por esquema, revisar:

- que la base definida en el YAML externo existe;
- que el usuario de PostgreSQL tiene permisos sobre esa base;
- que el script SQL del servicio se ejecuto contra la base correcta;
- que no hay una tabla antigua con estructura incompatible.

## Reset Limpio Opcional

Los scripts no destruyen datos por defecto. Usan `CREATE DATABASE` y `CREATE TABLE IF NOT EXISTS`.

Si una base quedo con una estructura incorrecta, hacer un reset solo despues de guardar los datos necesarios:

```powershell
dropdb -U postgres novabank_cuentas
psql -U postgres -f docs/sql/03-novabank-cuentas.sql
```

Repetir el mismo criterio con la base afectada.
